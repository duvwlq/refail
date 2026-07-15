export type AuthUser = {
  userId: number;
  email: string;
  nickname: string;
  role: "USER" | "ADMIN";
  status: "ACTIVE" | "RESTRICTED" | "DELETED";
  createdAt: string;
};

export type LoginResponse = {
  accessToken: string;
  tokenType: "Bearer";
  user: AuthUser;
};
