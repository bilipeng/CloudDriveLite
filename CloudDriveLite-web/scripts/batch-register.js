/**
 * 批量注册用户脚本
 * 使用方法：
 * 1. 确保后端服务正在运行
 * 2. 在项目根目录运行: node scripts/batch-register.js
 * 或者在浏览器控制台运行（需要先打开网站）
 */

// 配置
const BASE_URL = 'http://localhost:8080'; // 根据实际情况修改
const USER_COUNT = 200;
const DEFAULT_PASSWORD = '123456'; // 默认密码

// 生成随机字符串
function randomString(length, prefix = '') {
  const chars = 'abcdefghijklmnopqrstuvwxyz0123456789';
  let result = prefix;
  for (let i = 0; i < length; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return result;
}

// 生成随机手机号
function randomPhoneNumber() {
  const prefixes = ['130', '131', '132', '133', '134', '135', '136', '137', '138', '139',
                    '150', '151', '152', '153', '155', '156', '157', '158', '159',
                    '180', '181', '182', '183', '184', '185', '186', '187', '188', '189'];
  const prefix = prefixes[Math.floor(Math.random() * prefixes.length)];
  const suffix = String(Math.floor(Math.random() * 100000000)).padStart(8, '0');
  return prefix + suffix;
}

// 注册单个用户
async function registerUser(userData, index) {
  try {
    const params = new URLSearchParams({
      username: userData.username,
      userNumber: userData.userNumber,
      phoneNumber: userData.phoneNumber,
      password: userData.password,
    });
    
    if (userData.email) {
      params.append('email', userData.email);
    }

    const response = await fetch(`${BASE_URL}/api/auth/register?${params.toString()}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    const result = await response.json();
    
    if (response.ok && result.id) {
      return {
        success: true,
        index: index + 1,
        user: userData,
        userId: result.id,
        userNumber: result.userNumber
      };
    } else {
      return {
        success: false,
        index: index + 1,
        user: userData,
        error: result.message || '注册失败'
      };
    }
  } catch (error) {
    return {
      success: false,
      index: index + 1,
      user: userData,
      error: error.message
    };
  }
}

// 批量注册
async function batchRegister() {
  console.log(`开始批量注册 ${USER_COUNT} 个用户...\n`);
  
  const users = [];
  const results = {
    success: [],
    failed: []
  };

  // 生成用户数据
  for (let i = 0; i < USER_COUNT; i++) {
    const num = String(i + 1).padStart(2, '0');
    users.push({
      username: `testuser${num}`,
      userNumber: `user${num}${randomString(4)}`,
      phoneNumber: randomPhoneNumber(),
      email: `test${num}@example.com`,
      password: DEFAULT_PASSWORD
    });
  }

  // 逐个注册
  for (let i = 0; i < users.length; i++) {
    const result = await registerUser(users[i], i);
    
    if (result.success) {
      results.success.push(result);
      console.log(`✓ [${result.index}/${USER_COUNT}] 注册成功: ${result.user.username} (${result.userNumber})`);
    } else {
      results.failed.push(result);
      console.log(`✗ [${result.index}/${USER_COUNT}] 注册失败: ${result.user.username} - ${result.error}`);
    }
    
    // 添加延迟，避免请求过快
    if (i < users.length - 1) {
      await new Promise(resolve => setTimeout(resolve, 200));
    }
  }

  // 输出总结
  console.log('\n' + '='.repeat(60));
  console.log('注册完成！');
  console.log('='.repeat(60));
  console.log(`成功: ${results.success.length} 个`);
  console.log(`失败: ${results.failed.length} 个`);
  
  if (results.success.length > 0) {
    console.log('\n成功注册的用户:');
    results.success.forEach(r => {
      console.log(`  - ${r.user.username} (账号: ${r.userNumber}, 手机: ${r.user.phoneNumber})`);
    });
  }
  
  if (results.failed.length > 0) {
    console.log('\n注册失败的用户:');
    results.failed.forEach(r => {
      console.log(`  - ${r.user.username}: ${r.error}`);
    });
  }

  // 生成登录信息文件
  const loginInfo = results.success.map(r => ({
    username: r.user.username,
    userNumber: r.userNumber,
    phoneNumber: r.user.phoneNumber,
    password: r.user.password,
    email: r.user.email
  }));

  console.log('\n用户登录信息已生成，可以复制以下内容保存:');
  console.log(JSON.stringify(loginInfo, null, 2));

  return results;
}

// 运行脚本
if (typeof window === 'undefined') {
  // Node.js 环境
  batchRegister().catch(console.error);
} else {
  // 浏览器环境
  console.log('请在浏览器控制台运行 batchRegister() 函数');
  window.batchRegister = batchRegister;
}

