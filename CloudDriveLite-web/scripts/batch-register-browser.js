/**
 * 浏览器控制台批量注册脚本
 * 使用方法：
 * 1. 打开网站（确保后端服务运行）
 * 2. 打开浏览器开发者工具（F12）
 * 3. 复制整个脚本到控制台运行
 * 4. 调用 batchRegister() 函数
 */

(function() {
  'use strict';

  const USER_COUNT = 200;
  const DEFAULT_PASSWORD = '123456';

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

      const response = await fetch(`/api/auth/register?${params.toString()}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include'
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
  window.batchRegister = async function() {
    console.log(`%c开始批量注册 ${USER_COUNT} 个用户...`, 'font-size: 16px; font-weight: bold; color: #409EFF;');
    console.log('');
    
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
        console.log(`%c✓ [${result.index}/${USER_COUNT}] 注册成功: ${result.user.username} (${result.userNumber})`, 'color: #67C23A;');
      } else {
        results.failed.push(result);
        console.log(`%c✗ [${result.index}/${USER_COUNT}] 注册失败: ${result.user.username} - ${result.error}`, 'color: #F56C6C;');
      }
      
      // 添加延迟，避免请求过快
      if (i < users.length - 1) {
        await new Promise(resolve => setTimeout(resolve, 200));
      }
    }

    // 输出总结
    console.log('');
    console.log('%c' + '='.repeat(60), 'font-weight: bold;');
    console.log('%c注册完成！', 'font-size: 16px; font-weight: bold; color: #409EFF;');
    console.log('%c' + '='.repeat(60), 'font-weight: bold;');
    console.log(`%c成功: ${results.success.length} 个`, `color: #67C23A; font-weight: bold;`);
    console.log(`%c失败: ${results.failed.length} 个`, `color: #F56C6C; font-weight: bold;`);
    
    if (results.success.length > 0) {
      console.log('%c\n成功注册的用户:', 'font-weight: bold;');
      results.success.forEach(r => {
        console.log(`  - ${r.user.username} (账号: ${r.userNumber}, 手机: ${r.user.phoneNumber})`);
      });
    }
    
    if (results.failed.length > 0) {
      console.log('%c\n注册失败的用户:', 'font-weight: bold; color: #F56C6C;');
      results.failed.forEach(r => {
        console.log(`  - ${r.user.username}: ${r.error}`);
      });
    }

    // 生成登录信息
    const loginInfo = results.success.map(r => ({
      username: r.user.username,
      userNumber: r.userNumber,
      phoneNumber: r.user.phoneNumber,
      password: r.user.password,
      email: r.user.email
    }));

    console.log('%c\n用户登录信息:', 'font-weight: bold;');
    console.log(JSON.stringify(loginInfo, null, 2));

    // 保存到 window 对象，方便查看
    window.registeredUsers = loginInfo;
    console.log('\n%c提示: 用户信息已保存到 window.registeredUsers，可以在控制台查看', 'color: #909399; font-style: italic;');

    return results;
  };

  console.log('%c批量注册脚本已加载！', 'font-size: 14px; font-weight: bold; color: #67C23A;');
  console.log('调用 batchRegister() 开始注册 20 个用户');
  console.log('');
})();

