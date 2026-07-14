src/
├── app/
│   ├── (auth)/
│   │   ├── login/
│   │   │   └── page.tsx
│   │   ├── register/
│   │   │   └── page.tsx
│   │   └── layout.tsx
│   │
│   ├── (main)/
│   │   ├── layout.tsx
│   │   ├── courses/
│   │   │   ├── page.tsx
│   │   │   └── [courseId]/
│   │   │       └── page.tsx
│   │   ├── enrollments/
│   │   │   └── page.tsx
│   │   ├── goals/
│   │   │   ├── page.tsx
│   │   │   └── [goalId]/
│   │   │       └── page.tsx
│   │   ├── questions/
│   │   │   ├── page.tsx
│   │   │   └── [questionId]/
│   │   │       └── page.tsx
│   │   └── reviews/
│   │       └── page.tsx
│   │
│   ├── globals.css
│   └── layout.tsx
│
├── components/
│   ├── ui/
│   │   ├── Button.tsx
│   │   ├── Input.tsx
│   │   ├── Card.tsx
│   │   └── Modal.tsx
│   ├── auth/
│   ├── courses/
│   ├── goals/
│   ├── questions/
│   └── reviews/
│
├── lib/
│   ├── api/
│   │   ├── auth.ts
│   │   ├── courses.ts
│   │   ├── enrollments.ts
│   │   ├── goals.ts
│   │   ├── questions.ts
│   │   └── reviews.ts
│   └── axios.ts
│
├── store/
│   ├── authStore.ts
│   └── enrollmentStore.ts
│
├── types/
│   ├── auth.ts
│   ├── course.ts
│   ├── enrollment.ts
│   ├── goal.ts
│   ├── question.ts
│   └── review.ts
│
└── hooks/
    ├── useAuth.ts
    ├── useCourses.ts
    └── useGoals.ts