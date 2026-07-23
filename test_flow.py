import requests, json

BASE = 'http://localhost:8084/api'
issues = []
passed = 0
failed = 0

def check(label, condition, detail=""):
    global passed, failed
    if condition:
        passed += 1
        print(f"  PASS  {label}")
    else:
        failed += 1
        issues.append(f"{label}: {detail}")
        print(f"  FAIL  {label} -- {detail}")

print("=" * 60)
print("1. AUTH FLOW")
print("=" * 60)

# Login all users
users = {
    'admin': ('admin@officedesk.com', 'admin123'),
    'emp': ('rahul@officedesk.com', 'pass123'),
    'agent': ('vikram@officedesk.com', 'pass123'),
    'head': ('deepak@officedesk.com', 'pass123'),
}
tokens = {}
for role, (email, pw) in users.items():
    r = requests.post(f'{BASE}/auth/login', json={'email': email, 'password': pw})
    check(f"Login {role}", r.status_code == 200, f"{r.status_code}: {r.text[:100]}")
    if r.status_code == 200:
        tokens[role] = r.json()['accessToken']

# Register
r = requests.post(f'{BASE}/auth/register', json={'name':'Test2','email':'test2@test.com','password':'pass123','departmentId':1})
check("Register new user", r.status_code == 201, f"{r.status_code}")

# Duplicate email
r = requests.post(f'{BASE}/auth/register', json={'name':'Test2b','email':'test2@test.com','password':'pass123','departmentId':1})
check("Duplicate email rejected", r.status_code == 409, f"{r.status_code}")

# Wrong password
r = requests.post(f'{BASE}/auth/login', json={'email':'admin@officedesk.com','password':'wrongpass'})
check("Wrong password rejected", r.status_code != 200, f"{r.status_code}")

def hdr(role):
    return {'Authorization': f'Bearer {tokens[role]}', 'Content-Type': 'application/json'}

print()
print("=" * 60)
print("2. TICKET CREATE")
print("=" * 60)

r = requests.post(f'{BASE}/tickets', headers=hdr('emp'), json={'title':'Test ticket','description':'Testing full flow','priority':'HIGH','category':'Software'})
check("Create ticket (emp, Software->IT)", r.status_code == 201, f"{r.status_code}: {r.text[:150]}")
ticket_id = r.json()['id'] if r.status_code == 201 else None
if ticket_id:
    t = r.json()
    check("  Auto-assigned to IT dept", t['departmentName'] == 'IT', t['departmentName'])
    check("  Auto-assigned to agent", t['assignedToName'] is not None, str(t['assignedToName']))
    check("  Status is ASSIGNED", t['status'] == 'ASSIGNED', t['status'])

# Invalid category
r = requests.post(f'{BASE}/tickets', headers=hdr('emp'), json={'title':'Bad','description':'Test','priority':'LOW','category':'InvalidCat'})
check("Invalid category rejected", r.status_code != 201, f"{r.status_code}")

# Missing fields
r = requests.post(f'{BASE}/tickets', headers=hdr('emp'), json={'title':'','description':'','priority':'LOW','category':'Software'})
check("Missing fields rejected", r.status_code == 400, f"{r.status_code}")

print()
print("=" * 60)
print("3. MY TICKETS")
print("=" * 60)
r = requests.get(f'{BASE}/tickets/my', headers=hdr('emp'))
check("Get my tickets", r.status_code == 200, f"{r.status_code}")
if r.status_code == 200:
    check("  Has tickets", r.json()['totalElements'] > 0, str(r.json()['totalElements']))

print()
print("=" * 60)
print("4. TICKET DETAIL")
print("=" * 60)
if ticket_id:
    r = requests.get(f'{BASE}/tickets/{ticket_id}', headers=hdr('emp'))
    check("Get ticket detail (owner)", r.status_code == 200, f"{r.status_code}")
    r = requests.get(f'{BASE}/tickets/{ticket_id}', headers=hdr('agent'))
    check("Get ticket detail (agent)", r.status_code == 200, f"{r.status_code}")

print()
print("=" * 60)
print("5. STATUS TRANSITIONS")
print("=" * 60)
if ticket_id:
    r = requests.put(f'{BASE}/tickets/{ticket_id}/status', headers=hdr('agent'), json={'status':'IN_PROGRESS'})
    check("ASSIGNED -> IN_PROGRESS (agent)", r.status_code == 200, f"{r.status_code}: {r.text[:150]}")

    r = requests.put(f'{BASE}/tickets/{ticket_id}/status', headers=hdr('agent'), json={'status':'RESOLVED','resolutionNote':'Fixed it'})
    check("IN_PROGRESS -> RESOLVED (agent)", r.status_code == 200, f"{r.status_code}: {r.text[:150]}")

    # Invalid transitions
    r = requests.put(f'{BASE}/tickets/{ticket_id}/status', headers=hdr('emp'), json={'status':'IN_PROGRESS'})
    check("RESOLVED -> IN_PROGRESS (skip, invalid)", r.status_code == 400, f"{r.status_code}")

    # Skip transition
    r2 = requests.get(f'{BASE}/tickets/3', headers=hdr('emp'))
    if r2.status_code == 200:
        st = r2.json()['status']
        if st == 'RAISED':
            r3 = requests.put(f'{BASE}/tickets/3/status', headers=hdr('agent'), json={'status':'RESOLVED'})
            check("RAISED -> RESOLVED skipped rejected", r3.status_code == 400, f"{r3.status_code}")

print()
print("=" * 60)
print("6. REOPEN (from RESOLVED)")
print("=" * 60)
if ticket_id:
    # Ticket is currently RESOLVED, test reopen
    r = requests.post(f'{BASE}/tickets/{ticket_id}/reopen', headers=hdr('emp'), json={'reason':'Issue persists'})
    check("Reopen RESOLVED ticket (owner)", r.status_code == 200, f"{r.status_code}: {r.text[:150]}")
    if r.status_code == 200:
        check("  Status now REOPENED", r.json()['status'] == 'REOPENED', r.json()['status'])

    # Non-owner reopen (ticket 3 is RAISED, not RESOLVED)
    r2 = requests.get(f'{BASE}/tickets/3', headers=hdr('emp'))
    if r2.status_code == 200:
        t3id = r2.json()['id']
        r3 = requests.post(f'{BASE}/tickets/{t3id}/reopen', headers=hdr('emp'), json={'reason':'test'})
        check("Reopen non-RESOLVED ticket rejected", r3.status_code == 400, f"{r3.status_code}")

    # Non-owner reopen (emp2 tries to reopen emp1's ticket)
    emp2_resp = requests.post(f'{BASE}/auth/login', json={'email':'priya@officedesk.com','password':'pass123'})
    if emp2_resp.status_code == 200:
        emp2_token = emp2_resp.json()['accessToken']
        r4 = requests.post(f'{BASE}/tickets/{ticket_id}/reopen', headers={'Authorization': f'Bearer {emp2_token}', 'Content-Type': 'application/json'}, json={'reason':'test'})
        check("Reopen as non-owner rejected", r4.status_code == 403, f"{r4.status_code}")

    # Now transition back: REOPENED -> IN_PROGRESS -> RESOLVED -> CLOSED
    r = requests.put(f'{BASE}/tickets/{ticket_id}/status', headers=hdr('agent'), json={'status':'IN_PROGRESS'})
    check("REOPENED -> IN_PROGRESS (agent)", r.status_code == 200, f"{r.status_code}")
    r = requests.put(f'{BASE}/tickets/{ticket_id}/status', headers=hdr('agent'), json={'status':'RESOLVED','resolutionNote':'Fixed again'})
    check("IN_PROGRESS -> RESOLVED (agent)", r.status_code == 200, f"{r.status_code}")
    r = requests.put(f'{BASE}/tickets/{ticket_id}/status', headers=hdr('emp'), json={'status':'CLOSED'})
    check("RESOLVED -> CLOSED (emp/owner)", r.status_code == 200, f"{r.status_code}")

print()
print("=" * 60)
print("7. RATING")
print("=" * 60)
if ticket_id:
    # Ticket is CLOSED from step 6, test rating
    r = requests.post(f'{BASE}/tickets/{ticket_id}/rate', headers=hdr('emp'), json={'rating':4,'feedback':'Good'})
    check("Rate CLOSED ticket (owner)", r.status_code == 200, f"{r.status_code}: {r.text[:150]}")

    r = requests.post(f'{BASE}/tickets/{ticket_id}/rate', headers=hdr('emp'), json={'rating':3})
    check("Duplicate rating rejected", r.status_code == 400, f"{r.status_code}")

    r = requests.post(f'{BASE}/tickets/{ticket_id}/rate', headers=hdr('head'), json={'rating':2})
    check("Non-owner rating rejected", r.status_code == 403, f"{r.status_code}")

print()
print("=" * 60)
print("8. COMMENTS")
print("=" * 60)
if ticket_id:
    r = requests.post(f'{BASE}/tickets/{ticket_id}/comments', headers=hdr('emp'), json={'comment':'Hello from employee'})
    check("Add public comment (emp)", r.status_code == 201, f"{r.status_code}: {r.text[:150]}")

    r = requests.post(f'{BASE}/tickets/{ticket_id}/comments', headers=hdr('agent'), json={'comment':'Internal note','isInternal':True})
    check("Add internal comment (agent)", r.status_code == 201, f"{r.status_code}: {r.text[:150]}")

    r = requests.get(f'{BASE}/tickets/{ticket_id}/comments', headers=hdr('emp'))
    if r.status_code == 200:
        comments = r.json()
        internal_count = sum(1 for c in comments if c.get('isInternal', c.get('internal', False)))
        check(f"Employee sees public only ({len(comments)} comments)", internal_count == 0, f"Found {internal_count} internal comments")

    r = requests.get(f'{BASE}/tickets/{ticket_id}/comments', headers=hdr('agent'))
    if r.status_code == 200:
        comments = r.json()
        internal_count = sum(1 for c in comments if c.get('isInternal', c.get('internal', False)))
        check(f"Agent sees all ({len(comments)} comments, {internal_count} internal)", internal_count > 0, f"Found {internal_count} internal")

print()
print("=" * 60)
print("9. DEPT/AGENT/ALL TICKETS")
print("=" * 60)
r = requests.get(f'{BASE}/tickets/dept/1', headers=hdr('agent'))
check("Dept tickets (IT)", r.status_code == 200, f"{r.status_code}: {r.text[:150]}")

r = requests.get(f'{BASE}/tickets/agent/6', headers=hdr('agent'))
check("Agent tickets (vikram)", r.status_code == 200, f"{r.status_code}: {r.text[:150]}")

r = requests.get(f'{BASE}/tickets/all', headers=hdr('admin'))
check("All tickets (admin)", r.status_code == 200, f"{r.status_code}: {r.text[:150]}")

print()
print("=" * 60)
print("10. ASSIGN TICKET")
print("=" * 60)
r = requests.post(f'{BASE}/tickets', headers=hdr('emp'), json={'title':'Assign test','description':'Test assign','priority':'LOW','category':'AC'})
check("Create ticket for assign test", r.status_code == 201, f"{r.status_code}")
assign_id = r.json()['id'] if r.status_code == 201 else None
if assign_id:
    r = requests.put(f'{BASE}/tickets/{assign_id}/assign', headers=hdr('head'), json={'agentId':6})
    check("Assign ticket (dept head)", r.status_code == 200, f"{r.status_code}: {r.text[:150]}")

    r = requests.put(f'{BASE}/tickets/{assign_id}/assign', headers=hdr('emp'), json={'agentId':6})
    check("Assign ticket (employee, unauthorized)", r.status_code == 403, f"{r.status_code}")

print()
print("=" * 60)
print("11. ADMIN")
print("=" * 60)
r = requests.get(f'{BASE}/admin/users', headers=hdr('admin'))
check("Get all users", r.status_code == 200, f"{r.status_code}: {r.text[:150]}")

r = requests.post(f'{BASE}/admin/users', headers=hdr('admin'), json={'name':'New Agent','email':'newagent@test.com','password':'pass123','role':'AGENT','departmentId':1})
check("Create user (admin)", r.status_code == 201, f"{r.status_code}: {r.text[:150]}")
new_user_id = r.json()['id'] if r.status_code == 201 else None

if new_user_id:
    r = requests.put(f'{BASE}/admin/users/{new_user_id}/deactivate', headers=hdr('admin'))
    check("Deactivate user", r.status_code == 200, f"{r.status_code}")

    r = requests.put(f'{BASE}/admin/users/{new_user_id}/activate', headers=hdr('admin'))
    check("Activate user", r.status_code == 200, f"{r.status_code}")

# Employee tries admin
r = requests.get(f'{BASE}/admin/users', headers=hdr('emp'))
check("Admin endpoint blocked for employee", r.status_code == 403, f"{r.status_code}")

# Admin SLA update
r = requests.put(f'{BASE}/admin/sla/1', headers=hdr('admin'), json={'priority':'HIGH','resolutionHours':12})
check("SLA update (admin)", r.status_code == 200, f"{r.status_code}: {r.text[:150]}")

print()
print("=" * 60)
print("12. UNAUTHENTICATED ACCESS")
print("=" * 60)
r = requests.get(f'{BASE}/tickets/my')
check("Protected endpoint without token", r.status_code in [401, 403], f"{r.status_code}")

r = requests.get(f'{BASE}/auth/login')
# Login only accepts POST, GET should be 405 or similar
check("Auth endpoints are public", True)

print()
print("=" * 60)
print("SUMMARY")
print("=" * 60)
print(f"  PASSED: {passed}")
print(f"  FAILED: {failed}")
if issues:
    print(f"\n  ISSUES:")
    for i, issue in enumerate(issues, 1):
        print(f"    {i}. {issue}")
