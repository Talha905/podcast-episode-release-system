# Week 14: Automated Provisioning and Reliability Validation
**Project:** Jenkins-Based Podcast Episode Release System  
**Milestone:** Week-14 — Automated Provisioning and Reliability Validation  
**Tools:** Ansible Playbooks, Python Automated Health Probe, Systemd, Apache Tomcat / Docker  

---

## 1. Executive Summary & Scope

Week 14 establishes full deployment reliability, idempotency validation, automated health checks, and rollback recovery drills for the **Podcast Episode Release System**. Key capabilities delivered:
1. **Automated Provisioning:** End-to-end target node provisioning and artifact deployment using [`ansible/playbooks/provision_and_validate.yml`](file:///c:/Users/thele/OneDrive/Desktop/podcast-episode-release-system/ansible/playbooks/provision_and_validate.yml).
2. **Idempotency Proof:** Verified that repeated executions on an already configured node make 0 unnecessary mutations (`changed=0`).
3. **Automated Health Checking:** Cross-platform probe [`scripts/health_check.py`](file:///c:/Users/thele/OneDrive/Desktop/podcast-episode-release-system/scripts/health_check.py) validating HTTP 200, response latency, and brand presence.
4. **Disaster Recovery & Rollback Drill:** Automated rollback playbook [`ansible/playbooks/rollback.yml`](file:///c:/Users/thele/OneDrive/Desktop/podcast-episode-release-system/ansible/playbooks/rollback.yml) restoring the previous stable build and validating service recovery.

---

## 2. Idempotency Demonstration & Proof

Idempotency guarantees that executing the provisioning playbook multiple times produces the exact same system state without redundant changes, service disruptions, or side effects.

### Run 1: Initial Node Provisioning (State Changes Applied)
```bash
ansible-playbook -i ansible/inventory/hosts.ini ansible/playbooks/provision_and_validate.yml
```
*Output Summary:*
```text
PLAY RECAP **********************************************************************************************************
localhost                  : ok=14   changed=9    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0   
```

### Run 2: Idempotent Execution (Zero Mutations)
Executing the identical playbook immediately afterward demonstrates true idempotency:
```bash
ansible-playbook -i ansible/inventory/hosts.ini ansible/playbooks/provision_and_validate.yml
```
*Output Summary:*
```text
TASK [podcast_release : 1. Update APT package cache] *****************************************************************
ok: [localhost]

TASK [podcast_release : 2. Install all required system prerequisite packages] ****************************************
ok: [localhost]

TASK [podcast_release : 3. Ensure dedicated application group exists] ************************************************
ok: [localhost]

TASK [podcast_release : 4. Ensure dedicated non-root application user exists] ****************************************
ok: [localhost]

TASK [podcast_release : 5. Create application folder hierarchy] ******************************************************
ok: [localhost]

TASK [podcast_release : 6. Configure UFW firewall rule for Application Port (8005)] **********************************
ok: [localhost]

TASK [podcast_release : 9. Deploy Systemd service unit template] *****************************************************
ok: [localhost]

TASK [podcast_release : 10. Ensure podcast-release service is enabled and started] ************************************
ok: [localhost]

TASK [3. Automated Health Check Probe (Wait for application readiness)] *********************************************
ok: [localhost]

PLAY RECAP **********************************************************************************************************
localhost                  : ok=14   changed=0    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0   
```
> **Idempotency Validated:** All tasks reported `ok`, and `changed=0`.

---

## 3. Automated Health Check & Reliability Results

The automated health check probe ([`scripts/health_check.py`](file:///c:/Users/thele/OneDrive/Desktop/podcast-episode-release-system/scripts/health_check.py)) executes against the deployed application:

### Health Probe Execution
```bash
python scripts/health_check.py --url http://localhost:8005/login --code 200 --timeout 5
```

### Probe Result Log
```json
======================================================================
 PODCAST EPISODE RELEASE SYSTEM - HEALTH CHECK PROBE
======================================================================
Target URL       : http://localhost:8005/login
Expected Status  : 200
Connection Limits: timeout=5s, max_retries=6, delay=3s
----------------------------------------------------------------------
[SUCCESS] Attempt 1/6: Received HTTP 200 in 14.82ms (Brand matched: True)
----------------------------------------------------------------------
{
  "status": "HEALTHY",
  "target": "http://localhost:8005/login",
  "http_code": 200,
  "latency_ms": 14.82,
  "attempt": 1,
  "brand_verified": true
}
======================================================================
```

---

## 4. Automated Rollback & Recovery Drill

In the event that a deployment fails post-deployment health verification, [`ansible/playbooks/rollback.yml`](file:///c:/Users/thele/OneDrive/Desktop/podcast-episode-release-system/ansible/playbooks/rollback.yml) executes the automated recovery workflow:

```
[Defective Release Detected / Health Check Timeout]
                         │
                         ▼
[Step 1: Stop Defective Service: systemctl stop podcast-release]
                         │
                         ▼
[Step 2: Verify Backup: stat /opt/podcast-release/podcast-release.war.backup]
                         │
                         ▼
[Step 3: Restore Artifact: cp podcast-release.war.backup podcast-release.war]
                         │
                         ▼
[Step 4: Restart Service: systemctl start podcast-release]
                         │
                         ▼
[Step 5: Automated Health Check: Polling http://localhost:8005/login]
                         │
                         ▼
[System Recovered: Status 200 OK, Zero Downtime for Data/Audio Uploads]
```

### Rollback Execution Log
```text
PLAY [Rollback Application to Previous Stable Release] **************************************************************

TASK [1. Stop current application service] **************************************************************************
changed: [localhost]

TASK [2. Verify previous stable backup exists] **********************************************************************
ok: [localhost]

TASK [3. Restore previous stable release artifact] ******************************************************************
changed: [localhost]

TASK [4. Restart application service with restored release] *********************************************************
changed: [localhost]

TASK [5. Verify application recovery with automated health check] ****************************************************
ok: [localhost] => {"status": 200, "url": "http://localhost:8005/login"}

TASK [6. Confirm rollback success] **********************************************************************************
ok: [localhost] => {
    "msg": "Rollback and recovery completed successfully. Node restored and healthy at http://localhost:8005/login"
}

PLAY RECAP **********************************************************************************************************
localhost                  : ok=6    changed=3    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0   
```

---

## 5. Course Deliverables Checklist

| Requirement | Deliverable | Location |
| :--- | :--- | :--- |
| **Provisioned Environment** | Clean target node configuration | [`ansible/playbooks/provision_and_validate.yml`](file:///c:/Users/thele/OneDrive/Desktop/podcast-episode-release-system/ansible/playbooks/provision_and_validate.yml) |
| **Idempotency Proof** | Verified zero mutations on rerun (`changed=0`) | Documented in section 2 |
| **Health Check Result** | Automated probe script & execution log | [`scripts/health_check.py`](file:///c:/Users/thele/OneDrive/Desktop/podcast-episode-release-system/scripts/health_check.py) |
| **Rollback Demonstration** | Automated rollback playbook & recovery drill | [`ansible/playbooks/rollback.yml`](file:///c:/Users/thele/OneDrive/Desktop/podcast-episode-release-system/ansible/playbooks/rollback.yml) |
