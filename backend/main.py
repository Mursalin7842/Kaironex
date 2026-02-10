"""
Appwrite Functions Entry Point
"""
import sys
import os

# Add the function directory to Python path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from src.main import main

# Appwrite Functions expects a 'main' function at root level
