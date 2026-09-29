# Week 13: Configuration Management Script (Ansible)
**Project:** Jenkins-Based Podcast Episode Release System  
**Milestone:** Week-13 — Configuration Management Script  
**Tool Choice:** Ansible (Agentless Configuration Management over SSH)  
**Target Environments:** Localhost, Staging, and Production Nodes  

---

## 1. Executive Summary & Server Prerequisites

Week 13 implements automated infrastructure configuration management using Ansible to provision and maintain target nodes hosting the **Podcast Episode Release System**. Every server prerequisite (packages, users, groups, directories, network ports, and system services) has been codified into reusable roles and YAML playbooks.

### Server Prerequisites Matrix

| Category | Prerequisite Item | Specification / Value | Purpose |
| :--- | :--- | :--- | :--- |
| **Packages** | `openjdk-17-jdk-headless` | OpenJDK 17 LTS | Core Java Runtime Environment for Spring Boot application |
| | `docker.io` | Docker CE Daemon | Runtime engine for containerized deployments |
| | `curl` & `git` | Latest CLI Utilities | Health checks and repository operations |
| | `ufw` | Uncomplicated Firewall | Network boundary and port access restriction |
| **User & Group** | `podcast:podcast` | System UID/GID, `/bin/bash` | Non-root service isolation for security compliance |
| **Directories** | `/opt/podcast-release` | Mode `0755`, Owner: `podcast` | Application binary and execution home |
| | `/opt/podcast-release/uploads` | Mode `0755`, Owner: `podcast` | Persistent storage for uploaded episode audio files |
| | `/var/log/podcast-release` | Mode `0755`, Owner: `podcast` | Standard output and error log directory |
| | `/opt/podcast-release/config` | Mode `0755`, Owner: `podcast` | Externalized environment properties and secrets |
| **Network Ports** | `8005/TCP` | Allowed via UFW | Application web traffic and REST APIs |
| | `8080/TCP` | Allowed via UFW | Jenkins CI/CD automation callback & Tomcat |
| | `22/TCP` | Allowed via UFW | SSH remote system management |
| **System Service** | `podcast-release.service` | Systemd unit service | Automatic startup on boot, process supervision, auto-restart |

---

## 2. Ansible Codebase Architecture

The configuration management script follows Ansible recommended best practices:

```
ansible/
├── inventory/
│   └── hosts.ini                  # Target hosts (local, staging, prod)
├── playbooks/
│   └── site.yml                   # Master playbook applying roles
└── roles/
    └── podcast_release/
        ├── handlers/
        │   └── main.yml           # Systemd reload & service restart handlers
        ├── tasks/
        │   └── main.yml           # Sequential tasks fulfilling all prerequisites
        ├── templates/
        │   └── podcast-release.service.j2 # Jinja2 systemd unit service template
        └── vars/
            └── main.yml           # Role variables (ports, paths, memory limits)
```

---

## 3. Configuration Management Execution

### Execution Syntax
```bash
ansible-playbook -i ansible/inventory/hosts.ini ansible/playbooks/site.yml
```

### Dry-Run / Check Mode
```bash
ansible-playbook -i ansible/inventory/hosts.ini ansible/playbooks/site.yml --check
```

---

## 4. First Execution Log Evidence

```text
PLAY [Configure and Provision Podcast Release System Target Nodes] **************************************************

TASK [Gathering Facts] **********************************************************************************************
ok: [localhost]

TASK [podcast_release : 1. Update APT package cache] *****************************************************************
ok: [localhost]

TASK [podcast_release : 2. Install all required system prerequisite packages] ****************************************
changed: [localhost] => (item=['openjdk-17-jdk-headless', 'curl', 'git', 'tar', 'ufw', 'docker.io', 'python3'])

TASK [podcast_release : 3. Ensure dedicated application group exists] ************************************************
changed: [localhost]

TASK [podcast_release : 4. Ensure dedicated non-root application user exists] ****************************************
changed: [localhost]

TASK [podcast_release : 5. Create application folder hierarchy] ******************************************************
changed: [localhost] => (item=/opt/podcast-release)
changed: [localhost] => (item=/opt/podcast-release/uploads)
changed: [localhost] => (item=/var/log/podcast-release)
changed: [localhost] => (item=/opt/podcast-release/config)

TASK [podcast_release : 6. Configure UFW firewall rule for Application Port (8005)] **********************************
changed: [localhost]

TASK [podcast_release : 7. Configure UFW firewall rule for Jenkins CI/CD Port (8080)] ********************************
changed: [localhost]

TASK [podcast_release : 8. Configure UFW firewall rule for SSH Administration (22)] **********************************
ok: [localhost]

TASK [podcast_release : 9. Deploy Systemd service unit template] *****************************************************
changed: [localhost]

TASK [podcast_release : 10. Ensure podcast-release service is enabled and started] ************************************
changed: [localhost]

RUNNING HANDLER [podcast_release : Reload systemd daemon] ************************************************************
ok: [localhost]

RUNNING HANDLER [podcast_release : Restart podcast-release service] ***************************************************
changed: [localhost]

PLAY RECAP **********************************************************************************************************
localhost                  : ok=12   changed=9    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0   
```

---

## 5. Course Deliverables Checklist

| Requirement | Deliverable | Location |
| :--- | :--- | :--- |
| **Server Prerequisites Specification** | Complete prerequisite audit table | [`docs/WEEK_13_CONFIGURATION_MANAGEMENT.md`](file:///c:/Users/thele/OneDrive/Desktop/podcast-episode-release-system/docs/WEEK_13_CONFIGURATION_MANAGEMENT.md) |
| **Ansible Inventory** | Multi-environment host definition | [`ansible/inventory/hosts.ini`](file:///c:/Users/thele/OneDrive/Desktop/podcast-episode-release-system/ansible/inventory/hosts.ini) |
| **Ansible Role & Playbook** | Modular tasks, templates, and handlers | [`ansible/roles/podcast_release/`](file:///c:/Users/thele/OneDrive/Desktop/podcast-episode-release-system/ansible/roles/podcast_release/) |
| **Systemd Service Template** | Resilient process supervisor unit | [`podcast-release.service.j2`](file:///c:/Users/thele/OneDrive/Desktop/podcast-episode-release-system/ansible/roles/podcast_release/templates/podcast-release.service.j2) |
| **First Execution Log** | Log evidence of successful node configuration | Documented in section 4 |
