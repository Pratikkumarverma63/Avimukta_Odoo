import axios from 'axios';
import {
  ApiResponse,
  CustomerDashboard,
  DashboardMetrics,
  DiscountRule,
  Invoice,
  Notification,
  PageResponse,
  Payment,
  Plan,
  Product,
  ProductVariant,
  Quotation,
  QuotationTemplate,
  RevenueTrend,
  Subscription,
  TaxRule,
  User,
} from '../types';

const api = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      if (window.location.pathname !== '/login' && window.location.pathname !== '/register') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default api;

// 1. Auth APIs
export const authApi = {
  login: async (credentials: { email: string; password: string }) => {
    const res = await api.post<ApiResponse<import('../types').AuthResponse>>('/auth/login', credentials);
    return res.data;
  },
  register: async (data: { email: string; password: string; firstName: string; lastName: string; companyName?: string }) => {
    const res = await api.post<ApiResponse<import('../types').AuthResponse>>('/auth/register', data);
    return res.data;
  },
  forgotPassword: async (email: string) => {
    const res = await api.post<ApiResponse<import('../types').ForgotPasswordResponse>>('/auth/forgot-password', { email });
    return res.data;
  },
  resetPassword: async (data: { token: string; newPassword: string }) => {
    const res = await api.post<ApiResponse<string>>('/auth/reset-password', data);
    return res.data;
  },
  getCurrentUser: async () => {
    const res = await api.get<ApiResponse<import('../types').UserProfileResponse>>('/auth/me');
    return res.data;
  },
};

// 2. Dashboard APIs
export const dashboardApi = {
  getMetrics: async (params?: { startDate?: string; endDate?: string; customerId?: number }) => {
    const res = await api.get<ApiResponse<DashboardMetrics>>('/dashboard/metrics', { params });
    return res.data.data;
  },
  getRevenueTrend: async () => {
    const res = await api.get<ApiResponse<RevenueTrend>>('/dashboard/revenue-trend');
    return res.data.data;
  },
};

// 3. Products & Variants APIs
export const productApi = {
  getAll: async (params?: { page?: number; size?: number; search?: string }) => {
    const res = await api.get<ApiResponse<PageResponse<Product>>>('/products', { params });
    return res.data.data;
  },
  getById: async (id: number) => {
    const res = await api.get<ApiResponse<Product>>(`/products/${id}`);
    return res.data.data;
  },
  create: async (data: Partial<Product>) => {
    const res = await api.post<ApiResponse<Product>>('/products', data);
    return res.data.data;
  },
  update: async (id: number, data: Partial<Product>) => {
    const res = await api.put<ApiResponse<Product>>(`/products/${id}`, data);
    return res.data.data;
  },
  delete: async (id: number) => {
    const res = await api.delete<ApiResponse<void>>(`/products/${id}`);
    return res.data;
  },
  addVariant: async (productId: number, variant: Partial<ProductVariant>) => {
    const res = await api.post<ApiResponse<ProductVariant>>(`/products/${productId}/variants`, variant);
    return res.data.data;
  },
};

// 4. Subscription Plans APIs
export const planApi = {
  getAll: async (params?: { page?: number; size?: number }) => {
    const res = await api.get<ApiResponse<PageResponse<Plan>>>('/plans', { params });
    return res.data.data;
  },
  getById: async (id: number) => {
    const res = await api.get<ApiResponse<Plan>>(`/plans/${id}`);
    return res.data.data;
  },
  create: async (data: Partial<Plan>) => {
    const res = await api.post<ApiResponse<Plan>>('/plans', data);
    return res.data.data;
  },
  update: async (id: number, data: Partial<Plan>) => {
    const res = await api.put<ApiResponse<Plan>>(`/plans/${id}`, data);
    return res.data.data;
  },
  delete: async (id: number) => {
    const res = await api.delete<ApiResponse<void>>(`/plans/${id}`);
    return res.data;
  },
};

// 5. Quotation Templates & Quotations APIs
export const quotationApi = {
  getTemplates: async () => {
    const res = await api.get<ApiResponse<QuotationTemplate[]>>('/quotation-templates');
    return res.data.data;
  },
  createTemplate: async (data: Partial<QuotationTemplate>) => {
    const res = await api.post<ApiResponse<QuotationTemplate>>('/quotation-templates', data);
    return res.data.data;
  },
  getAll: async (params?: { page?: number; size?: number }) => {
    const res = await api.get<ApiResponse<PageResponse<Quotation>>>('/quotations', { params });
    return res.data.data;
  },
  getById: async (id: number) => {
    const res = await api.get<ApiResponse<Quotation>>(`/quotations/${id}`);
    return res.data.data;
  },
  create: async (data: any) => {
    const res = await api.post<ApiResponse<Quotation>>('/quotations', data);
    return res.data.data;
  },
  confirm: async (id: number) => {
    const res = await api.post<ApiResponse<Subscription>>(`/quotations/${id}/confirm`);
    return res.data.data;
  },
};

// 6. Subscriptions APIs
export const subscriptionApi = {
  getAll: async (params?: { page?: number; size?: number; status?: string; customerId?: number }) => {
    const res = await api.get<ApiResponse<PageResponse<Subscription>>>('/subscriptions', { params });
    return res.data.data;
  },
  getById: async (id: number) => {
    const res = await api.get<ApiResponse<Subscription>>(`/subscriptions/${id}`);
    return res.data.data;
  },
  create: async (data: any) => {
    const res = await api.post<ApiResponse<Subscription>>('/subscriptions', data);
    return res.data.data;
  },
  pause: async (id: number) => {
    const res = await api.post<ApiResponse<Subscription>>(`/subscriptions/${id}/pause`);
    return res.data.data;
  },
  resume: async (id: number) => {
    const res = await api.post<ApiResponse<Subscription>>(`/subscriptions/${id}/resume`);
    return res.data.data;
  },
  close: async (id: number) => {
    const res = await api.post<ApiResponse<Subscription>>(`/subscriptions/${id}/close`);
    return res.data.data;
  },
  renew: async (id: number) => {
    const res = await api.post<ApiResponse<Subscription>>(`/subscriptions/${id}/renew`);
    return res.data.data;
  },
};

// 7. Invoices APIs
export const invoiceApi = {
  getAll: async (params?: { page?: number; size?: number; status?: string }) => {
    const res = await api.get<ApiResponse<PageResponse<Invoice>>>('/invoices', { params });
    return res.data.data;
  },
  getById: async (id: number) => {
    const res = await api.get<ApiResponse<Invoice>>(`/invoices/${id}`);
    return res.data.data;
  },
  getOverdue: async () => {
    const res = await api.get<ApiResponse<Invoice[]>>('/invoices/overdue');
    return res.data.data;
  },
  getPrintableHtml: async (id: number) => {
    const res = await api.get<string>(`/invoices/${id}/printable`, {
      headers: { Accept: 'text/html, text/plain, */*' },
      responseType: 'text',
    });
    return res.data;
  },
  confirm: async (id: number) => {
    const res = await api.post<ApiResponse<Invoice>>(`/invoices/${id}/confirm`);
    return res.data.data;
  },
  cancel: async (id: number) => {
    const res = await api.post<ApiResponse<Invoice>>(`/invoices/${id}/cancel`);
    return res.data.data;
  },
};

// 8. Payments APIs
export const paymentApi = {
  getAll: async (params?: { page?: number; size?: number }) => {
    const res = await api.get<ApiResponse<PageResponse<Payment>>>('/payments', { params });
    return res.data.data;
  },
  recordPayment: async (data: {
    invoiceId: number;
    amount: number;
    paymentMethod: string;
    transactionReference?: string;
    notes?: string;
  }) => {
    const res = await api.post<ApiResponse<Payment>>('/payments', data);
    return res.data.data;
  },
  getByInvoice: async (invoiceId: number) => {
    const res = await api.get<ApiResponse<Payment[]>>(`/payments/invoice/${invoiceId}`);
    return res.data.data;
  },
};

// 9. Taxes & Discounts APIs
export const taxApi = {
  getAll: async () => {
    const res = await api.get<ApiResponse<TaxRule[]>>('/taxes');
    return res.data.data;
  },
  create: async (data: Partial<TaxRule>) => {
    const res = await api.post<ApiResponse<TaxRule>>('/taxes', data);
    return res.data.data;
  },
  update: async (id: number, data: Partial<TaxRule>) => {
    const res = await api.put<ApiResponse<TaxRule>>(`/taxes/${id}`, data);
    return res.data.data;
  },
};

export const discountApi = {
  getAll: async () => {
    const res = await api.get<ApiResponse<DiscountRule[]>>('/discounts');
    return res.data.data;
  },
  create: async (data: Partial<DiscountRule>) => {
    const res = await api.post<ApiResponse<DiscountRule>>('/discounts', data);
    return res.data.data;
  },
  update: async (id: number, data: Partial<DiscountRule>) => {
    const res = await api.put<ApiResponse<DiscountRule>>(`/discounts/${id}`, data);
    return res.data.data;
  },
};

// 10. Billing Engine Trigger API
export const billingApi = {
  triggerRun: async (asOfDate?: string) => {
    const res = await api.post<ApiResponse<any>>('/billing/trigger', null, {
      params: asOfDate ? { asOfDate } : {},
    });
    return res.data.data;
  },
};

// 11. Customer Portal APIs
export const portalApi = {
  getDashboard: async () => {
    const res = await api.get<ApiResponse<CustomerDashboard>>('/customer/dashboard');
    return res.data.data;
  },
  getSubscriptions: async () => {
    const res = await api.get<ApiResponse<Subscription[]>>('/customer/subscriptions');
    return res.data.data;
  },
  getInvoices: async () => {
    const res = await api.get<ApiResponse<Invoice[]>>('/customer/invoices');
    return res.data.data;
  },
  getNotifications: async () => {
    const res = await api.get<ApiResponse<Notification[]>>('/customer/notifications');
    return res.data.data;
  },
  markNotificationRead: async (id: number) => {
    const res = await api.put<ApiResponse<void>>(`/customer/notifications/${id}/read`);
    return res.data;
  },
  subscribe: async (planId: number, variantId?: number, quantity: number = 1) => {
    const params = new URLSearchParams();
    params.append('planId', planId.toString());
    if (variantId) params.append('variantId', variantId.toString());
    params.append('quantity', quantity.toString());
    const res = await api.post<ApiResponse<Subscription>>(`/customer/subscribe?${params.toString()}`);
    return res.data.data;
  },
  getAvailablePlans: async () => {
    const res = await api.get<ApiResponse<PageResponse<Plan>>>('/plans?activeOnly=true&size=50');
    return res.data.data.content;
  },
  updateProfile: async (data: { companyName: string; contactName: string; phone?: string; billingAddress?: string; taxId?: string }) => {
    const res = await api.put<ApiResponse<CustomerDashboard>>('/customer/profile', data);
    return res.data.data;
  },
  changePassword: async (data: { currentPassword: string; newPassword: string }) => {
    const res = await api.put<ApiResponse<void>>('/customer/change-password', data);
    return res.data;
  },
  cancelSubscription: async (subscriptionId: number, reason?: string) => {
    const params = new URLSearchParams();
    params.append('subscriptionId', subscriptionId.toString());
    if (reason) params.append('reason', reason);
    const res = await api.post<ApiResponse<Subscription>>(`/customer/cancel-subscription?${params.toString()}`);
    return res.data.data;
  },
};

// 12. Admin Staff APIs
export const staffApi = {
  getAll: async () => {
    const res = await api.get<ApiResponse<User[]>>('/admin/staff');
    return res.data.data;
  },
  create: async (data: { email: string; password: string; firstName: string; lastName: string }) => {
    const res = await api.post<ApiResponse<User>>('/admin/staff', data);
    return res.data.data;
  },
};

// 13. Proration Engine APIs (Unique Hackathon Feature)
export const prorationApi = {
  calculate: async (subscriptionId: number, data: import('../types').ProrationCalculationRequest) => {
    const res = await api.post<ApiResponse<import('../types').ProrationCalculationResponse>>(
      `/subscriptions/${subscriptionId}/prorate/calculate`,
      data
    );
    return res.data.data;
  },
  apply: async (subscriptionId: number, data: import('../types').ProrationCalculationRequest) => {
    const res = await api.post<ApiResponse<import('../types').ProrationCalculationResponse>>(
      `/subscriptions/${subscriptionId}/prorate/apply`,
      data
    );
    return res.data.data;
  },
  getHistory: async (subscriptionId: number) => {
    const res = await api.get<ApiResponse<import('../types').ProrationRecord[]>>(
      `/subscriptions/${subscriptionId}/prorate/history`
    );
    return res.data.data;
  },
};

// 14. Smart Dunning & Revenue Recovery APIs (Unique Hackathon Feature)
export const dunningApi = {
  getCases: async () => {
    const res = await api.get<ApiResponse<import('../types').DunningCase[]>>('/dunning/cases');
    return res.data.data;
  },
  process: async () => {
    const res = await api.post<ApiResponse<string>>('/dunning/process');
    return res.data;
  },
};
