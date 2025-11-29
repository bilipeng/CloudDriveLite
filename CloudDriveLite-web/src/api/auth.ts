import request from './request'

// 用户登录接口
export interface LoginParams {
  userNumber: string
  password: string
}

export interface LoginResponse {
  message: string
  userId: number
  userName: string
}

// 用户注册接口
export interface RegisterParams {
  username: string
  userNumber: string
  phoneNumber: string
  password: string
  email?: string
}

export interface RegisterResponse {
  id: number
  userNumber: string
}

// 找回密码接口
export interface ForgotPasswordParams {
  userNumber: string
  phoneNumber?: string
  email?: string
  newPassword: string
}

export interface ForgotPasswordResponse {
  message: string
}




// 认证相关API
export const authApi = {
  // 用户登录
  login: (params: LoginParams): Promise<LoginResponse> => {
    return request.post('/api/auth/login', null, {
      params: {
        userNumber: params.userNumber,
        password: params.password

      }
    })
  },

  

  // 用户注册
  register: (params: RegisterParams): Promise<RegisterResponse> => {
    return request.post('/api/auth/register', null, {
      params: {
        username: params.username,
        userNumber: params.userNumber,
        phoneNumber: params.phoneNumber,
        password: params.password,
        email: params.email
      }
    })
  },

  // 用户登出
  logout: (): Promise<{ message: string }> => {
    return request.post('/api/auth/logout')
  },

  // 找回密码
  forgotPassword: (params: ForgotPasswordParams): Promise<ForgotPasswordResponse> => {
    const requestParams: any = {
      userNumber: params.userNumber,
      newPassword: params.newPassword
    }
    
    if (params.phoneNumber) {
      requestParams.phoneNumber = params.phoneNumber
    }
    if (params.email) {
      requestParams.email = params.email
    }
    
    return request.post('/api/auth/forgot-password', null, {
      params: requestParams
    })
  }
}
