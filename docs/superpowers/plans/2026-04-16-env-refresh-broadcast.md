# Environment Refresh Broadcast Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Broadcast Windows environment changes after registry writes so newly launched terminals can resolve updated tool paths without waiting for Explorer or logoff.

**Architecture:** Keep the existing registry write flow unchanged and add a single broadcast hook in the Windows environment command layer. Invoke that hook after successful machine/user environment synchronization, and log refresh failures without turning a successful registry write into an install failure.

**Tech Stack:** Java 17, Maven, JUnit 4, Windows `reg`, PowerShell, user32 `SendMessageTimeout`

---
