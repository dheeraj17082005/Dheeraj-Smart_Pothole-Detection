#!/usr/bin/env python3
"""
Functionality Inventory Generator for PotholeX
Scans frontend routes, API endpoints, backend controllers, role annotations, and entities.
"""

import os
import re

ROOT_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FRONTEND_DIR = os.path.join(ROOT_DIR, "frontend", "src")
BACKEND_DIR = os.path.join(ROOT_DIR, "backend", "src", "main", "java", "com", "pothole")

def scan_backend_endpoints():
    endpoints = []
    controller_dir = os.path.join(BACKEND_DIR, "controller")
    if not os.path.exists(controller_dir):
        return endpoints

    for root, _, files in os.walk(controller_dir):
        for f in files:
            if f.endswith(".java"):
                path = os.path.join(root, f)
                with open(path, "r", encoding="utf-8") as fp:
                    content = fp.read()
                    matches = re.findall(r'@(GetMapping|PostMapping|PatchMapping|PutMapping|DeleteMapping)\((?:value\s*=\s*)?["\']([^"\']+)["\']', content)
                    role_match = re.findall(r'@PreAuthorize\("([^"]+)"\)', content)
                    for method, endpoint_path in matches:
                        http_method = method.replace("Mapping", "").upper()
                        auth_rule = role_match[0] if role_match else "AUTHENTICATED / PERMIT_ALL"
                        endpoints.append({
                            "controller": f,
                            "method": http_method,
                            "path": endpoint_path,
                            "auth": auth_rule
                        })
    return endpoints

def scan_frontend_routes():
    routes = []
    app_file = os.path.join(FRONTEND_DIR, "App.tsx")
    if os.path.exists(app_file):
        with open(app_file, "r", encoding="utf-8") as fp:
            content = fp.read()
            matches = re.findall(r'<Route\s+path=["\']([^"\']+)["\']\s+element=\{<ProtectedRoute(?:\s+allowedRoles=\{([^}]+)\})?>?<([^/>]+)', content)
            for path, role, component in matches:
                routes.append({
                    "path": path,
                    "role": role.strip() if role else "ANY_AUTHENTICATED",
                    "component": component.strip()
                })
    return routes

def main():
    print("=== PotholeX Functionality Inventory Audit ===")
    endpoints = scan_backend_endpoints()
    routes = scan_frontend_routes()

    print(f"\nFound {len(endpoints)} Backend REST Endpoints:")
    for ep in endpoints:
        print(f"  [{ep['method']}] {ep['path']} -> {ep['controller']} ({ep['auth']})")

    print(f"\nFound {len(routes)} Frontend Routes:")
    for r in routes:
        print(f"  Route: {r['path']} -> {r['component']} (Allowed Roles: {r['role']})")

if __name__ == "__main__":
    main()
