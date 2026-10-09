#!/bin/bash
set -e

echo "========================================================"
echo "       Samba Sandbox - Initializing Test Data           "
echo "========================================================"

echo ">>> [1/3] Создание тестовых групп..."
groupadd developers
groupadd qa
groupadd hr
groupadd finance
groupadd managers
groupadd interns

echo ">>> [2/3] Создание тестовых пользователей..."
TEST_PASS="test"

setup_user() {
    local username=$1
    local group=$2
    
    # Создаем системного пользователя без возможности входа по SSH (no shell)
    # и назначаем ему основную группу.
    useradd -m -s /usr/sbin/nologin -g "$group" "$username"
    
    # Устанавливаем пароль только для Samba
    (echo "$TEST_PASS"; echo "$TEST_PASS") | smbpasswd -s -a "$username"
    smbpasswd -e "$username"
}

setup_user "dev1" "developers"
setup_user "dev2" "developers"
setup_user "qa1" "qa"
setup_user "qa2" "qa"
setup_user "hr1" "hr"
setup_user "fin1" "finance"
setup_user "boss" "managers"
setup_user "intern1" "interns"

# Пользователь admin (созданный ранее) добавляется во все группы для полного доступа
for g in developers qa hr finance managers interns; do
    usermod -aG "$g" admin
done

echo ">>> [3/3] Создание директорий и генерация тестовых файлов..."
mkdir -p /srv/samba/developers
mkdir -p /srv/samba/qa
mkdir -p /srv/samba/hr
mkdir -p /srv/samba/finance
mkdir -p /srv/samba/management

# Устанавливаем владельца и группу. Флаг 2 (SetGID) гарантирует, что 
# новые файлы в этих папках унаследуют группу директории.
chown root:developers /srv/samba/developers
chmod 2770 /srv/samba/developers

chown root:qa /srv/samba/qa
chmod 2770 /srv/samba/qa

chown root:hr /srv/samba/hr
chmod 2770 /srv/samba/hr

chown root:finance /srv/samba/finance
chmod 2770 /srv/samba/finance

chown root:managers /srv/samba/management
chmod 2770 /srv/samba/management

# Генерация файлов

# Developers
echo "public static void main(String[] args) {}" > /srv/samba/developers/Main.java
echo "console.log('hello world');" > /srv/samba/developers/app.js
echo "# Project Architecture" > /srv/samba/developers/architecture.md
echo "docker-compose up -d" > /srv/samba/developers/deploy.sh
touch /srv/samba/developers/database-dump.sql

# QA
echo "Test case 1: Login success" > /srv/samba/qa/test_cases_v1.xlsx
echo "Automated UI tests" > /srv/samba/qa/cypress_suite.js
touch /srv/samba/qa/bug_report_1304.pdf
touch /srv/samba/qa/performance_results.csv

# HR
touch "/srv/samba/hr/Employee Handbook 2026.pdf"
touch "/srv/samba/hr/Q3_Team_Building_Budget.xlsx"
touch "/srv/samba/hr/Offer_Template.docx"
echo "John Doe,Jane Smith" > "/srv/samba/hr/candidates_list.csv"

# Finance
touch "/srv/samba/finance/Q3_Revenue_Report.xlsx"
touch "/srv/samba/finance/Tax_Declaration_2025.pdf"
touch "/srv/samba/finance/Invoice_#48992.pdf"
touch "/srv/samba/finance/Budget_Forecast_2027.xlsx"
touch "/srv/samba/finance/Audit_Results_Internal.docx"

# Management
touch "/srv/samba/management/Company_Strategy_2030.pptx"
touch "/srv/samba/management/Board_Meeting_Notes.docx"
touch "/srv/samba/management/Partnership_Agreement.pdf"

# Назначаем права на созданные файлы, чтобы они принадлежали правильным группам
chown -R root:developers /srv/samba/developers/*
chown -R root:qa /srv/samba/qa/*
chown -R root:hr /srv/samba/hr/*
chown -R root:finance /srv/samba/finance/*
chown -R root:managers /srv/samba/management/*

echo "========================================================"
echo " Тестовые пользователи и папки успешно созданы!         "
echo " Добавлены mock-файлы: pdf, docx, xlsx, pptx, csv       "
echo " Пользователи: dev1, dev2, qa1, qa2, hr1, fin1, boss, intern1"
echo " Пароль для всех тестовых аккаунтов: test"
echo "========================================================"
