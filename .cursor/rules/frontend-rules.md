# Frontend — React / TypeScript Coding Rules

> Applies to all `.tsx`, `.ts`, and `.css` files inside the `frontend/` directory.

---

## Folder Structure

Strictly follow this structure inside `frontend/src/`. Do not create folders outside this layout.

```
src/
├── api/                  # Axios instance + all API call functions
│   ├── axiosInstance.ts  # Single configured Axios instance
│   └── endpoints/        # One file per domain: tasks.api.ts, auth.api.ts
├── components/           # Reusable UI components (no page-level logic)
│   └── ui/               # shadcn/ui generated components — DO NOT modify
├── hooks/                # Custom React hooks only
├── pages/                # One file per route/page
├── store/                # Redux slices and store configuration
│   ├── index.ts          # Store setup
│   └── slices/           # authSlice.ts, themeSlice.ts
├── types/                # TypeScript interfaces and type aliases
├── schemas/              # Zod validation schemas
├── utils/                # Pure utility functions (no React, no side effects)
└── constants/            # App-wide constants: routes, query keys, enums
    ├── queryKeys.ts
    └── routes.ts
```

---

## TypeScript Rules

TypeScript is used in **strict mode**. There are no exceptions.

**Rules:**
- `strict: true` must be set in `tsconfig.json` at all times — never disable it
- **Never use `any`** — use `unknown` and narrow it, or define a proper type
- Never use type assertions (`as SomeType`) unless unavoidable — add a comment explaining why
- Always define **explicit return types** on all functions and hooks
- Use `interface` for object shapes that may be extended; use `type` for unions and aliases
- All API response shapes must have a corresponding interface in `src/types/`

```typescript
// ✅ CORRECT — explicit types everywhere
interface TaskResponse {
  id: number;
  title: string;
  status: TaskStatus;
  priority: Priority;
  assigneeId: number | null;
  dueDate: string | null;
  createdAt: string;
  updatedAt: string;
}

function formatDueDate(dueDate: string | null): string {
  if (!dueDate) return 'No due date';
  return new Date(dueDate).toLocaleDateString();
}

// ❌ WRONG
const task: any = await fetchTask(id);
const response = await api.get('/tasks'); // missing type
```

---

## Component Rules

One concern per component. One component per file.

**Rules:**
- One component per file — always, no exceptions
- File name must match the component name exactly in PascalCase: `TaskCard.tsx`
- Functional components only — no class components
- Always destructure props with an **explicit TypeScript interface**
- Keep components under **150 lines** — extract sub-components if exceeded
- Never put API calls directly in components — always use a custom hook
- Never put business logic in components — delegate to hooks or utils

```typescript
// ✅ CORRECT
interface TaskCardProps {
  task: TaskResponse;
  onStatusChange: (id: number, status: TaskStatus) => void;
  onDelete: (id: number) => void;
}

export function TaskCard({ task, onStatusChange, onDelete }: TaskCardProps): JSX.Element {
  return (
    <div className="rounded-lg border p-4 shadow-sm dark:border-gray-700">
      <h3 className="font-semibold text-gray-900 dark:text-gray-100">{task.title}</h3>
      {/* rest of JSX */}
    </div>
  );
}

// ❌ WRONG — no types, default export, API call inside component
export default function({ task }) {
  const [data, setData] = useState(null);
  useEffect(() => {
    fetch('/api/tasks').then(r => r.json()).then(setData);
  }, []);
}
```

---

## Custom Hook Rules

Custom hooks isolate logic from UI. They are the bridge between components and data.

**Rules:**
- All custom hooks must start with the `use` prefix
- One hook per file; file name matches the hook name: `useTasks.ts`
- Hooks must return a **typed object** — not an array (except when mimicking `useState` pattern)
- Never call hooks conditionally
- Always handle `isLoading`, `isError`, and success states in hooks that make API calls

```typescript
// ✅ CORRECT — typed return object, all states handled
interface UseTasksResult {
  tasks: TaskResponse[];
  isLoading: boolean;
  isError: boolean;
  error: Error | null;
}

export function useTasks(): UseTasksResult {
  const { data, isLoading, isError, error } = useQuery({
    queryKey: [QUERY_KEYS.TASKS],
    queryFn: taskApi.getAll,
    staleTime: 1000 * 60 * 5, // 5 minutes
  });

  return {
    tasks: data ?? [],
    isLoading,
    isError,
    error: error as Error | null,
  };
}
```

---

## React Query Rules

React Query owns **all server state**. Redux never touches server data.

**Rules:**
- All server state (tasks, users, notifications) must be managed by React Query
- Define all query keys as constants in `src/constants/queryKeys.ts`
- Always provide a `staleTime` to prevent unnecessary refetches
- Use `useMutation` for all create, update, and delete operations
- Always **invalidate related queries** after a successful mutation
- Show loading skeletons for `isLoading` states, not spinners blocking the whole page

```typescript
// ✅ src/constants/queryKeys.ts
export const QUERY_KEYS = {
  TASKS: 'tasks',
  TASK_DETAIL: (id: number) => ['tasks', id] as const,
  USERS: 'users',
  NOTIFICATIONS: 'notifications',
} as const;

// ✅ Mutation with cache invalidation and toast
export function useCreateTask(): UseMutationResult<TaskResponse, Error, CreateTaskRequest> {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: taskApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUERY_KEYS.TASKS] });
      toast.success('Task created successfully');
    },
    onError: (error) => {
      toast.error(`Failed to create task: ${error.message}`);
    },
  });
}
```

---

## Redux Rules

Redux manages **client-only global state**. Nothing from the server belongs here.

| State type | Where it lives |
|-----------|---------------|
| Server data (tasks, users) | React Query |
| Auth (user object, access token) | Redux |
| UI theme (dark/light) | Redux |
| Sidebar open/closed | Redux |
| Modal open/closed | `useState` (local) |
| Form step | `useState` (local) |

**Rules:**
- Use **Redux Toolkit only** — never write raw Redux reducers or action creators
- One slice per domain: `authSlice.ts`, `themeSlice.ts`
- Always use `createSelector` for derived/computed state to memoize selectors
- Selectors live in the same file as the slice

```typescript
// ✅ src/store/slices/authSlice.ts
interface AuthState {
  user: UserResponse | null;
  accessToken: string | null;
}

const initialState: AuthState = {
  user: null,
  accessToken: null,
};

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setCredentials: (state, action: PayloadAction<AuthState>) => {
      state.user = action.payload.user;
      state.accessToken = action.payload.accessToken;
    },
    logout: (state) => {
      state.user = null;
      state.accessToken = null;
    },
  },
});

export const { setCredentials, logout } = authSlice.actions;
export const selectCurrentUser = (state: RootState) => state.auth.user;
export const selectIsAuthenticated = createSelector(
  selectCurrentUser,
  (user) => user !== null
);
export default authSlice.reducer;
```

---

## Form Rules

Every form uses React Hook Form with Zod validation. No exceptions.

**Rules:**
- Always use **React Hook Form** — never use raw controlled inputs with `useState`
- Always pair with **Zod schema** via `@hookform/resolvers/zod`
- Define all Zod schemas in `src/schemas/` — never inline them in components
- Always show **field-level error messages** directly below each input
- Disable the submit button while the form is submitting

```typescript
// ✅ src/schemas/task.schema.ts
export const createTaskSchema = z.object({
  title: z
    .string()
    .min(1, 'Title is required')
    .max(255, 'Title must not exceed 255 characters'),
  description: z
    .string()
    .max(2000, 'Description must not exceed 2000 characters')
    .optional(),
  priority: z.nativeEnum(Priority, { required_error: 'Priority is required' }),
  dueDate: z.string().datetime({ message: 'Invalid date format' }).optional(),
  assigneeId: z.number().optional(),
});

export type CreateTaskFormValues = z.infer<typeof createTaskSchema>;

// ✅ Usage in component
const form = useForm<CreateTaskFormValues>({
  resolver: zodResolver(createTaskSchema),
  defaultValues: { title: '', priority: Priority.MEDIUM },
});
```

---

## API Layer Rules

All API communication goes through one Axios instance.

**Rules:**
- All API calls must go through `src/api/axiosInstance.ts` — never use raw `fetch`
- The Axios instance must have a **request interceptor** to attach the JWT token
- The Axios instance must have a **response interceptor** to handle 401 and trigger token refresh
- Group API functions by domain in `src/api/endpoints/` — one file per service
- All API functions must have **explicit parameter types and return types**

```typescript
// ✅ src/api/axiosInstance.ts
const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000,
});

axiosInstance.interceptors.request.use((config) => {
  const token = store.getState().auth.accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

axiosInstance.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    if (error.response?.status === 401) {
      // trigger refresh token logic
    }
    return Promise.reject(error);
  }
);

// ✅ src/api/endpoints/tasks.api.ts
export const taskApi = {
  getAll: (): Promise<TaskResponse[]> =>
    axiosInstance.get('/api/tasks').then((res) => res.data),

  getById: (id: number): Promise<TaskResponse> =>
    axiosInstance.get(`/api/tasks/${id}`).then((res) => res.data),

  create: (data: CreateTaskRequest): Promise<TaskResponse> =>
    axiosInstance.post('/api/tasks', data).then((res) => res.data),

  updateStatus: (id: number, status: TaskStatus): Promise<TaskResponse> =>
    axiosInstance.patch(`/api/tasks/${id}/status`, { status }).then((res) => res.data),

  delete: (id: number): Promise<void> =>
    axiosInstance.delete(`/api/tasks/${id}`).then((res) => res.data),
};
```

---

## Styling Rules

- Use **Tailwind CSS utility classes only** — no custom CSS files except `index.css` for base styles
- Never use inline styles (`style={{}}`) except for truly dynamic values (e.g. computed widths for charts)
- Use **shadcn/ui components** for all common UI elements: buttons, inputs, dialogs, tables, toasts, dropdowns
- **Do not modify** files inside `src/components/ui/` — these are shadcn generated
- Responsive design is **mandatory**: every page must work on mobile (375px) and desktop (1280px)
- Use Tailwind `dark:` variants on all color classes to support dark mode

```tsx
// ✅ CORRECT
<button className="bg-blue-600 hover:bg-blue-700 dark:bg-blue-500 dark:hover:bg-blue-600 text-white px-4 py-2 rounded-md text-sm font-medium transition-colors">
  Create Task
</button>

// ❌ WRONG — inline styles, no dark mode
<button style={{ backgroundColor: 'blue', color: 'white', padding: '8px 16px' }}>
  Create Task
</button>
```

---

## Error Handling Rules

- Every page component must be wrapped in an **error boundary**
- React Query errors must be caught and displayed with a **user-friendly message** — never show raw error objects
- Never show stack traces to the user
- Use **shadcn/ui Toast** for success and error notifications on all mutations
- Always handle the loading state with a **skeleton** (not just a spinner) for better UX

---

## Performance Rules

| Scenario | Tool |
|----------|------|
| Expensive calculation inside component | `useMemo` |
| Callback function passed to child component | `useCallback` |
| Pure presentational component with complex props | `React.memo` |
| Page-level component | `React.lazy` + `Suspense` |

```typescript
// ✅ Lazy load all pages
const TaskListPage = lazy(() => import('./pages/TaskListPage'));
const DashboardPage = lazy(() => import('./pages/DashboardPage'));

// In router
<Suspense fallback={<PageSkeleton />}>
  <Routes>
    <Route path="/dashboard" element={<DashboardPage />} />
    <Route path="/tasks" element={<TaskListPage />} />
  </Routes>
</Suspense>
```

---

## Protected Routes

All authenticated routes must be wrapped in a `ProtectedRoute` component that checks auth state from Redux and redirects to `/login` if not authenticated.

```typescript
// ✅ src/components/ProtectedRoute.tsx
export function ProtectedRoute({ children }: { children: JSX.Element }): JSX.Element {
  const isAuthenticated = useAppSelector(selectIsAuthenticated);

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return children;
}
```
