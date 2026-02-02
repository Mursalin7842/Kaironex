"""
📍 LOCAL RADIUS ENGINE
======================
Everything about the user's physical environment.

Features:
- Safe housing & utility tracking
- Local services scan (pharmacy, barber, grocery, laundromat)
- Quiet cafes & study spots
- Relaxation & refreshment spots
- Walking routes & exercise paths
- Culture & safety awareness

"We know your physical world so you don't have to map it yourself."
"""

import json
from typing import Dict, Any, List, Optional, Tuple
from dataclasses import dataclass, field, asdict
from datetime import datetime, time
from enum import Enum


class PlaceCategory(str, Enum):
    """Categories of local places."""
    PHARMACY = "pharmacy"
    GROCERY = "grocery"
    LAUNDROMAT = "laundromat"
    BARBER = "barber"
    CAFE = "cafe"
    LIBRARY = "library"
    GYM = "gym"
    RESTAURANT = "restaurant"
    FAST_FOOD = "fast_food"
    PARK = "park"
    STUDY_SPOT = "study_spot"
    MEDICAL = "medical"
    BANK = "bank"
    POST_OFFICE = "post_office"
    CAMPUS_BUILDING = "campus_building"


class PlaceVibe(str, Enum):
    """Vibe/atmosphere of a place."""
    QUIET = "quiet"
    MODERATE = "moderate"
    LIVELY = "lively"
    FOCUSED = "focused"
    SOCIAL = "social"
    RELAXING = "relaxing"


@dataclass
class LocalPlace:
    """A local place in user's radius."""
    place_id: str
    name: str
    category: PlaceCategory
    address: str
    distance_minutes_walk: int
    distance_minutes_transit: Optional[int] = None
    vibe: PlaceVibe = PlaceVibe.MODERATE
    student_friendly: bool = True
    has_wifi: bool = False
    has_outlets: bool = False
    budget_friendly: bool = True
    hours: Optional[Dict[str, str]] = None  # {"monday": "9:00-21:00", ...}
    notes: str = ""
    rating: float = 4.0
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            **asdict(self),
            "category": self.category.value,
            "vibe": self.vibe.value
        }


@dataclass
class Housing:
    """User's housing information."""
    address: str
    rent_monthly: float
    lease_end_date: Optional[str] = None
    utilities_included: bool = False
    roommates: int = 0
    distance_to_campus_minutes: int = 0
    transit_options: List[str] = field(default_factory=list)
    
    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


@dataclass
class UtilityTracker:
    """Track utility bills and due dates."""
    utility_type: str  # electricity, water, gas, internet, phone
    provider: str
    monthly_estimate: float
    due_day: int  # Day of month
    auto_pay: bool
    last_paid_date: Optional[str] = None
    account_number: Optional[str] = None
    
    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


@dataclass 
class WalkingRoute:
    """A walking route for exercise or relaxation."""
    route_id: str
    name: str
    duration_minutes: int
    distance_km: float
    difficulty: str  # easy, moderate, challenging
    highlights: List[str]
    best_time: str  # morning, evening, anytime
    has_shade: bool
    has_water_fountain: bool
    safety_notes: str = ""
    
    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


class LocalRadiusEngine:
    """
    Manages knowledge of user's physical environment.
    
    Features:
    1. Local services discovery
    2. Study spot recommendations
    3. Housing & utility tracking
    4. Walking routes & exercise spots
    5. Safety awareness
    """
    
    def __init__(self, db=None):
        self.db = db
        self._user_housing: Dict[str, Housing] = {}
        self._user_utilities: Dict[str, List[UtilityTracker]] = {}
        self._local_places: Dict[str, List[LocalPlace]] = {}
        self._walking_routes: Dict[str, List[WalkingRoute]] = {}
    
    def set_housing(self, user_id: str, housing: Housing):
        """Set user's housing information."""
        self._user_housing[user_id] = housing
    
    def add_utility(self, user_id: str, utility: UtilityTracker):
        """Add utility to track."""
        if user_id not in self._user_utilities:
            self._user_utilities[user_id] = []
        self._user_utilities[user_id].append(utility)
    
    def get_upcoming_bills(self, user_id: str, days_ahead: int = 14) -> List[Dict[str, Any]]:
        """Get bills due in the next N days."""
        if user_id not in self._user_utilities:
            return []
        
        today = datetime.now().day
        upcoming = []
        
        for utility in self._user_utilities[user_id]:
            days_until = (utility.due_day - today) % 30
            if days_until <= days_ahead:
                upcoming.append({
                    "utility": utility.utility_type,
                    "provider": utility.provider,
                    "amount": utility.monthly_estimate,
                    "due_in_days": days_until,
                    "auto_pay": utility.auto_pay,
                    "action_needed": not utility.auto_pay
                })
        
        return sorted(upcoming, key=lambda x: x["due_in_days"])
    
    def add_local_place(self, user_id: str, place: LocalPlace):
        """Add a local place to user's known radius."""
        if user_id not in self._local_places:
            self._local_places[user_id] = []
        self._local_places[user_id].append(place)
    
    def find_places(
        self,
        user_id: str,
        category: Optional[PlaceCategory] = None,
        max_distance_minutes: int = 30,
        needs_wifi: bool = False,
        needs_outlets: bool = False,
        vibe: Optional[PlaceVibe] = None,
        budget_friendly: bool = False
    ) -> List[LocalPlace]:
        """Find places matching criteria."""
        if user_id not in self._local_places:
            return []
        
        results = self._local_places[user_id]
        
        if category:
            results = [p for p in results if p.category == category]
        
        results = [p for p in results if p.distance_minutes_walk <= max_distance_minutes]
        
        if needs_wifi:
            results = [p for p in results if p.has_wifi]
        
        if needs_outlets:
            results = [p for p in results if p.has_outlets]
        
        if vibe:
            results = [p for p in results if p.vibe == vibe]
        
        if budget_friendly:
            results = [p for p in results if p.budget_friendly]
        
        return sorted(results, key=lambda p: p.distance_minutes_walk)
    
    def find_study_spots(
        self,
        user_id: str,
        duration_minutes: int = 60,
        needs_quiet: bool = True,
        needs_caffeine: bool = False
    ) -> List[Dict[str, Any]]:
        """Find good study spots based on needs."""
        candidates = []
        
        # Libraries
        libraries = self.find_places(
            user_id,
            category=PlaceCategory.LIBRARY,
            needs_wifi=True
        )
        for lib in libraries:
            candidates.append({
                "place": lib.to_dict(),
                "score": 90 if needs_quiet else 80,
                "pros": ["Free", "Quiet", "WiFi", "Outlets"],
                "cons": ["No food/drinks usually"]
            })
        
        # Cafes
        if needs_caffeine or not needs_quiet:
            cafes = self.find_places(
                user_id,
                category=PlaceCategory.CAFE,
                needs_wifi=True,
                vibe=PlaceVibe.QUIET if needs_quiet else None
            )
            for cafe in cafes:
                score = 85 if cafe.vibe == PlaceVibe.QUIET else 70
                candidates.append({
                    "place": cafe.to_dict(),
                    "score": score,
                    "pros": ["Coffee", "WiFi", "Comfortable"],
                    "cons": ["May need to buy something", "Time limits possible"]
                })
        
        return sorted(candidates, key=lambda x: x["score"], reverse=True)
    
    def suggest_walk(
        self,
        user_id: str,
        available_minutes: int,
        purpose: str = "refresh"  # refresh, exercise, explore
    ) -> Optional[Dict[str, Any]]:
        """Suggest a walking route."""
        if user_id not in self._walking_routes:
            return None
        
        # Find routes that fit the time
        suitable = [
            r for r in self._walking_routes[user_id]
            if r.duration_minutes <= available_minutes
        ]
        
        if not suitable:
            return None
        
        # Select based on purpose
        if purpose == "exercise":
            suitable.sort(key=lambda r: r.distance_km, reverse=True)
        elif purpose == "refresh":
            # Prefer shorter, relaxing routes
            suitable.sort(key=lambda r: r.duration_minutes)
        
        route = suitable[0]
        
        return {
            "route": route.to_dict(),
            "benefits": self._get_walk_benefits(purpose),
            "suggestion": f"A {route.duration_minutes}-minute walk through {route.name} would be perfect right now."
        }
    
    def get_safety_tips(self, user_id: str) -> Dict[str, Any]:
        """Get local safety information."""
        housing = self._user_housing.get(user_id)
        
        tips = {
            "general": [
                "Save campus security number in your phone",
                "Know the location of blue light emergency phones",
                "Walk in well-lit areas at night",
                "Use campus escort service for late nights",
                "Share location with trusted contact when out late"
            ],
            "home_safety": [
                "Always lock doors, even when home",
                "Don't let strangers into building",
                "Know emergency exits in your building",
                "Keep important documents in safe place"
            ],
            "emergency_contacts": {
                "emergency": "911",
                "campus_security": "Check your university website",
                "non_emergency_police": "Check local number",
                "poison_control": "1-800-222-1222"
            }
        }
        
        if housing and housing.distance_to_campus_minutes > 30:
            tips["commuter_tips"] = [
                "Know the last bus/train times",
                "Have backup transit options",
                "Keep phone charged for navigation",
                "Consider carpooling for late classes"
            ]
        
        return tips
    
    def get_essential_services(self, user_id: str) -> Dict[str, Any]:
        """Get essential nearby services organized by category."""
        essentials = {
            "healthcare": self.find_places(user_id, PlaceCategory.MEDICAL, max_distance_minutes=60),
            "pharmacy": self.find_places(user_id, PlaceCategory.PHARMACY),
            "grocery": self.find_places(user_id, PlaceCategory.GROCERY),
            "laundry": self.find_places(user_id, PlaceCategory.LAUNDROMAT),
            "bank": self.find_places(user_id, PlaceCategory.BANK),
            "post_office": self.find_places(user_id, PlaceCategory.POST_OFFICE)
        }
        
        return {k: [p.to_dict() for p in v] for k, v in essentials.items()}
    
    def _get_walk_benefits(self, purpose: str) -> List[str]:
        """Get benefits message for walking."""
        benefits = {
            "refresh": [
                "Clears mental fog",
                "Boosts creativity",
                "Reduces stress hormones",
                "Improves focus for next task"
            ],
            "exercise": [
                "Burns calories",
                "Improves cardiovascular health",
                "Releases endorphins",
                "Better sleep tonight"
            ],
            "explore": [
                "Discover new spots",
                "Build mental map of area",
                "Find hidden gems",
                "Feel more at home in your city"
            ]
        }
        return benefits.get(purpose, benefits["refresh"])
