"""
🌍 INTERNATIONAL STUDENT SUPPORT ENGINE
========================================
Everything an international student needs to survive and thrive.

Features:
- Visa status & renewal tracking
- Work hour compliance monitoring  
- Cultural guidance & local customs
- Slang dictionary (words not in textbooks)
- Admin protocols & deadlines
- Legal work limits by visa type

"We help you not break the law and fit in."
"""

import json
from typing import Dict, Any, List, Optional
from dataclasses import dataclass, field, asdict
from datetime import datetime, date, timedelta
from enum import Enum


class VisaType(str, Enum):
    """Common student visa types."""
    # US
    F1 = "F1"           # US student visa
    J1 = "J1"           # US exchange visitor
    # UK
    TIER4 = "TIER4"     # UK student visa
    # Canada
    STUDY_PERMIT = "STUDY_PERMIT"
    # Australia
    SUBCLASS_500 = "SUBCLASS_500"
    # Generic
    DOMESTIC = "DOMESTIC"
    OTHER = "OTHER"


class CulturalContext(str, Enum):
    """Cultural context situations."""
    CLASSROOM = "classroom"
    WORKPLACE = "workplace"
    SOCIAL = "social"
    FORMAL = "formal"
    CASUAL = "casual"


@dataclass
class VisaStatus:
    """Visa status tracking."""
    visa_type: VisaType
    start_date: date
    expiry_date: date
    work_hours_allowed: int
    can_work_off_campus: bool
    renewal_required_days_before: int = 90
    special_conditions: List[str] = field(default_factory=list)
    
    @property
    def days_until_expiry(self) -> int:
        return (self.expiry_date - date.today()).days
    
    @property
    def needs_renewal_attention(self) -> bool:
        return self.days_until_expiry <= self.renewal_required_days_before
    
    @property
    def is_expired(self) -> bool:
        return self.days_until_expiry < 0
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            **asdict(self),
            "visa_type": self.visa_type.value,
            "start_date": self.start_date.isoformat(),
            "expiry_date": self.expiry_date.isoformat(),
            "days_until_expiry": self.days_until_expiry,
            "needs_renewal_attention": self.needs_renewal_attention
        }


@dataclass
class SlangEntry:
    """Local slang dictionary entry."""
    term: str
    meaning: str
    context: CulturalContext
    region: str
    example: str
    avoid_using: bool = False  # Some terms are OK to understand but not use
    formality: str = "casual"  # casual, neutral, formal
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            **asdict(self),
            "context": self.context.value
        }


@dataclass
class CulturalTip:
    """Cultural guidance tip."""
    title: str
    description: str
    context: CulturalContext
    do_items: List[str]
    dont_items: List[str]
    why_it_matters: str


class InternationalStudentEngine:
    """
    Complete support for international students.
    
    Features:
    1. Visa tracking & renewal alerts
    2. Work hour compliance 
    3. Cultural guidance
    4. Local slang dictionary
    5. Admin protocol reminders
    """
    
    def __init__(self, db=None):
        self.db = db
        self._visa_info: Dict[str, VisaStatus] = {}
        self._slang_db = self._init_slang_database()
        self._cultural_tips = self._init_cultural_tips()
    
    def set_visa_info(self, user_id: str, visa_status: VisaStatus):
        """Set user's visa information."""
        self._visa_info[user_id] = visa_status
    
    def check_visa_status(self, user_id: str) -> Dict[str, Any]:
        """Check visa status and any required actions."""
        if user_id not in self._visa_info:
            return {"status": "not_set", "message": "Please set your visa information."}
        
        visa = self._visa_info[user_id]
        
        result = {
            "visa": visa.to_dict(),
            "alerts": [],
            "actions_required": []
        }
        
        if visa.is_expired:
            result["alerts"].append({
                "severity": "critical",
                "message": "⚠️ VISA EXPIRED! Contact your international student office IMMEDIATELY."
            })
            result["actions_required"].append("Contact international student office today")
        
        elif visa.needs_renewal_attention:
            result["alerts"].append({
                "severity": "warning",
                "message": f"Visa expires in {visa.days_until_expiry} days. Start renewal process."
            })
            result["actions_required"].extend([
                "Schedule appointment with international student office",
                "Gather required documents",
                "Check processing times"
            ])
        
        elif visa.days_until_expiry <= 180:
            result["alerts"].append({
                "severity": "info",
                "message": f"Visa expires in {visa.days_until_expiry} days. Keep it on your radar."
            })
        
        return result
    
    def get_work_rules(self, visa_type: VisaType) -> Dict[str, Any]:
        """Get work rules for a visa type."""
        
        rules = {
            VisaType.F1: {
                "max_hours_during_school": 20,
                "max_hours_during_breaks": 40,
                "on_campus_allowed": True,
                "off_campus_requires": "CPT or OPT authorization",
                "restrictions": [
                    "Cannot work off-campus without authorization",
                    "Must maintain full-time enrollment",
                    "Work must not interfere with studies"
                ],
                "tips": [
                    "On-campus jobs don't require special authorization",
                    "Apply for CPT before starting internships",
                    "Keep records of all hours worked"
                ]
            },
            VisaType.J1: {
                "max_hours_during_school": 20,
                "max_hours_during_breaks": 40,
                "on_campus_allowed": True,
                "off_campus_requires": "Sponsor approval",
                "restrictions": [
                    "Need sponsor approval for off-campus work",
                    "Some J1 categories have additional limits"
                ],
                "tips": [
                    "Contact your sponsor for work authorization",
                    "Keep your DS-2019 current"
                ]
            },
            VisaType.TIER4: {
                "max_hours_during_school": 20,
                "max_hours_during_breaks": 40,
                "on_campus_allowed": True,
                "off_campus_requires": "No extra authorization needed",
                "restrictions": [
                    "Cannot be self-employed",
                    "Cannot work as professional sportsperson",
                    "Cannot work as entertainer"
                ],
                "tips": [
                    "Keep payslips as proof of hours",
                    "Check your BRP for work conditions"
                ]
            },
            VisaType.STUDY_PERMIT: {
                "max_hours_during_school": 20,
                "max_hours_during_breaks": 40,
                "on_campus_allowed": True,
                "off_campus_requires": "Study permit with work authorization",
                "restrictions": [
                    "Must be enrolled full-time at DLI",
                    "Cannot start working before studies begin"
                ],
                "tips": [
                    "Get your SIN number before starting work",
                    "Keep enrollment letters handy"
                ]
            }
        }
        
        return rules.get(visa_type, {
            "max_hours_during_school": 20,
            "warning": "Unknown visa type - check with your international office"
        })
    
    def lookup_slang(
        self,
        term: Optional[str] = None,
        context: Optional[CulturalContext] = None,
        region: Optional[str] = None
    ) -> List[SlangEntry]:
        """Look up local slang terms."""
        results = self._slang_db
        
        if term:
            results = [s for s in results if term.lower() in s.term.lower()]
        
        if context:
            results = [s for s in results if s.context == context]
        
        if region:
            results = [s for s in results if region.lower() in s.region.lower()]
        
        return results
    
    def get_cultural_tips(
        self,
        context: CulturalContext
    ) -> List[CulturalTip]:
        """Get cultural tips for a context."""
        return [t for t in self._cultural_tips if t.context == context]
    
    def get_admin_checklist(self, visa_type: VisaType) -> Dict[str, Any]:
        """Get admin tasks checklist for international students."""
        
        checklist = {
            "on_arrival": [
                "Report to international student office within 15 days",
                "Get Social Security Number (if working)",
                "Set up bank account",
                "Get phone number",
                "Register with home country embassy (optional)",
                "Get student ID card"
            ],
            "ongoing": [
                "Keep passport valid (at least 6 months)",
                "Maintain full-time enrollment",
                "Report address changes to school",
                "Keep I-20/DS-2019/visa documents safe",
                "Track work hours if employed"
            ],
            "before_travel": [
                "Get travel signature on I-20 (if applicable)",
                "Check visa validity for re-entry",
                "Carry enrollment verification letter",
                "Have financial documents ready"
            ],
            "renewals": [
                {
                    "item": "Visa stamp",
                    "when": "Before travel if expired",
                    "lead_time_days": 60
                },
                {
                    "item": "I-20/DS-2019",
                    "when": "Before expiration",
                    "lead_time_days": 90
                },
                {
                    "item": "Passport",
                    "when": "6+ months before expiry",
                    "lead_time_days": 180
                }
            ]
        }
        
        return checklist
    
    def _init_slang_database(self) -> List[SlangEntry]:
        """Initialize common slang database (US focused example)."""
        return [
            SlangEntry(
                term="bet",
                meaning="Agreement, like saying 'okay' or 'sounds good'",
                context=CulturalContext.CASUAL,
                region="US",
                example="'Want to grab lunch?' 'Bet!'",
                avoid_using=False
            ),
            SlangEntry(
                term="lowkey",
                meaning="Secretly or somewhat; understated",
                context=CulturalContext.CASUAL,
                region="US",
                example="I lowkey want to skip class today",
                avoid_using=False
            ),
            SlangEntry(
                term="highkey",
                meaning="Openly, obviously, very much",
                context=CulturalContext.CASUAL,
                region="US",
                example="I highkey aced that exam",
                avoid_using=False
            ),
            SlangEntry(
                term="no cap",
                meaning="No lie, for real, being honest",
                context=CulturalContext.CASUAL,
                region="US",
                example="That was the best pizza ever, no cap",
                avoid_using=False
            ),
            SlangEntry(
                term="sus",
                meaning="Suspicious or questionable",
                context=CulturalContext.CASUAL,
                region="US/Global",
                example="That excuse seems kinda sus",
                avoid_using=False
            ),
            SlangEntry(
                term="slay",
                meaning="To do something excellently, look amazing",
                context=CulturalContext.CASUAL,
                region="US",
                example="You're going to slay that presentation",
                avoid_using=False
            ),
            SlangEntry(
                term="ghosting",
                meaning="Stopping all communication without explanation",
                context=CulturalContext.SOCIAL,
                region="US/Global",
                example="He ghosted me after two dates",
                avoid_using=False
            ),
            SlangEntry(
                term="hit different",
                meaning="Affects you in a unique or special way",
                context=CulturalContext.CASUAL,
                region="US",
                example="Coffee in the morning hits different",
                avoid_using=False
            ),
            SlangEntry(
                term="mid",
                meaning="Average, mediocre, nothing special",
                context=CulturalContext.CASUAL,
                region="US",
                example="That movie was kinda mid honestly",
                avoid_using=False
            ),
            SlangEntry(
                term="bussin",
                meaning="Really good, especially for food",
                context=CulturalContext.CASUAL,
                region="US",
                example="This food is bussin!",
                avoid_using=False
            )
        ]
    
    def _init_cultural_tips(self) -> List[CulturalTip]:
        """Initialize cultural guidance tips."""
        return [
            CulturalTip(
                title="Office Hours Culture",
                description="Professors expect students to visit during office hours",
                context=CulturalContext.CLASSROOM,
                do_items=[
                    "Visit office hours - it's expected and appreciated",
                    "Prepare specific questions beforehand",
                    "Email ahead if you need longer than 15 minutes"
                ],
                dont_items=[
                    "Wait until exam week to meet professors",
                    "Skip office hours entirely - professors notice",
                    "Send long emails that should be office hour discussions"
                ],
                why_it_matters="Building professor relationships helps with recommendations, research opportunities, and understanding material better."
            ),
            CulturalTip(
                title="Small Talk Basics",
                description="Americans engage in casual conversation with strangers",
                context=CulturalContext.SOCIAL,
                do_items=[
                    "Respond to 'How are you?' with 'Good, thanks! You?'",
                    "Comment on weather, sports, or shared situation",
                    "Smile and make brief eye contact"
                ],
                dont_items=[
                    "Give detailed honest answers to 'How are you?'",
                    "Ignore people who greet you",
                    "Jump straight to business without brief greeting"
                ],
                why_it_matters="Small talk is social lubricant - it's not fake, it's how connections start."
            ),
            CulturalTip(
                title="Tipping Culture",
                description="Tipping is expected for many services in the US",
                context=CulturalContext.SOCIAL,
                do_items=[
                    "Tip 15-20% at restaurants",
                    "Tip $1-2 per drink at bars",
                    "Tip delivery drivers 15-20%",
                    "Tip hairdressers/barbers 15-20%"
                ],
                dont_items=[
                    "Skip tipping at sit-down restaurants",
                    "Argue about tipping culture",
                    "Tip at fast food or counter service (optional)"
                ],
                why_it_matters="Service workers rely on tips as significant portion of income. Not tipping is seen as insulting."
            ),
            CulturalTip(
                title="Email Etiquette",
                description="Professional email communication norms",
                context=CulturalContext.FORMAL,
                do_items=[
                    "Use 'Dear Professor [Last Name]' for faculty",
                    "Include clear subject line",
                    "Be concise but polite",
                    "Sign with your full name and class info"
                ],
                dont_items=[
                    "Use 'Hey' or first names unless invited",
                    "Send one-word emails",
                    "Expect immediate responses",
                    "Use excessive emojis in professional emails"
                ],
                why_it_matters="Email is often first impression - poor emails can affect how you're perceived."
            ),
            CulturalTip(
                title="Workplace Communication",
                description="How to communicate at internships/jobs",
                context=CulturalContext.WORKPLACE,
                do_items=[
                    "Ask questions when unsure",
                    "Give updates on your progress proactively",
                    "Speak up in meetings when you have input",
                    "Accept feedback gracefully"
                ],
                dont_items=[
                    "Stay silent when you don't understand",
                    "Wait to be checked on - be proactive",
                    "Take criticism personally",
                    "Over-apologize - it can seem insincere"
                ],
                why_it_matters="American workplace values initiative and clear communication. Silence is often interpreted negatively."
            )
        ]
