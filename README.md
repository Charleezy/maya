# Maya - AI Task Scheduler

A service that integrates AI chatbots with task scheduling and time management features. This application allows users to schedule tasks and set timers through their existing AI chatbots (like Nomi or Kindroid) and integrates with Google Calendar for task management.

## Environment Setup

Before running the application, you need to set up the following environment variables either through the console or in application.properties-local.yml:

```bash
# Nomi API Configuration
NOMI_API_KEY=your-nomi-api-key-here

# Database Configuration
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your-postgres-password-here

# Google Cloud Configuration
GOOGLE_APPLICATION_CREDENTIALS=path/to/your/google-cloud-credentials.json
```

You can set these environment variables in several ways:
1. Create a `.env` file in the root directory (recommended for development)
2. Set them in your system environment
3. Pass them as command-line arguments when running the application

When developing in local use the "Dspring-boot.run.profiles=local" runtime variable

### Google Cloud Setup

1. Create a Google Cloud project or use an existing one
2. Enable the Cloud Natural Language API:
   - Go to [Google Cloud Console](https://console.cloud.google.com)
   - Navigate to "APIs & Services" > "Library"
   - Search for "Cloud Natural Language API"
   - Click "Enable"
3. Create service account credentials:
   - Go to "APIs & Services" > "Credentials"
   - Click "Create Credentials" > "Service Account"
   - Fill in the service account details
   - Download the JSON key file
4. Set the `GOOGLE_APPLICATION_CREDENTIALS` environment variable:
   ```powershell
   # Windows PowerShell
   $env:GOOGLE_APPLICATION_CREDENTIALS="C:\path\to\your\credentials.json"
   
   # Linux/MacOS
   export GOOGLE_APPLICATION_CREDENTIALS="/path/to/your/credentials.json"
   ```

**Important**: Never commit your actual API keys, credentials, or passwords to version control!

## Features

- Integration with AI chatbots for natural language task scheduling
- Google Calendar integration for task management
- Pomodoro timer functionality with AI follow-ups
- RESTful API for chatbot integration
- Web interface for task management

## Tech Stack

### Backend
- Java Spring Boot
- Spring Security
- Google Calendar API
- PostgreSQL
- LangChain4j for NLP

### Frontend
- React
- Material-UI
- TypeScript

## Getting Started

1. Clone the repository
2. Set up environment variables as described above
3. Start PostgreSQL database
4. Run the backend application:
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```

## Project Structure

```
maya/
├── backend/           # Spring Boot application
├── frontend/          # React application
├── docs/             # Documentation
└── docker/           # Docker configuration files
```

## Prerequisites

- Java 17 or higher
- Node.js 18 or higher
- PostgreSQL
- Google Cloud Platform account (for Calendar API)
- Nomi API key 

TODO: look into using spacy over google cloud NLP later