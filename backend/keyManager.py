from flask import Flask, request, jsonify
import uuid
import time
import hashlib
import requests
import os
import json
from datetime import datetime, timedelta

app = Flask(__name__)

# 配置文件路径
CONFIG_FILE = "auth_data.json"

# 实际的 API 密钥（管理员密钥）
REAL_API_KEY = "sk-FL1MNDnGyTuV361N5Bdo7xkF1ucpBq9cUNqmORoAMRuBHHi7"

# 默认授权码数据
default_data = {
    # 授权码信息
    "authorization_codes": {
        # 固定授权码，用于测试（设备限制型）
        "YUYAN-TRIAL-001": {"max_usage": 1, "expiry": None, "active": True, "type": "device"},
        "YUYAN-TRIAL-002": {"max_usage": 2, "expiry": None, "active": True, "type": "device"},
        "YUYAN-TRIAL-003": {"max_usage": 3, "expiry": None, "active": True, "type": "device"},
        # 余额式授权码示例（余额型）
        "YUYAN-BALANCE-001": {"max_usage": 10, "used": 0, "expiry": None, "active": True, "type": "balance"},
    },
    # 设备使用记录
    "user_usage": {},
    # API 调用计数
    "api_call_count": 0
}

# 加载配置
def load_config():
    try:
        if os.path.exists(CONFIG_FILE):
            with open(CONFIG_FILE, 'r', encoding='utf-8') as f:
                return json.load(f)
        else:
            # 如果文件不存在，使用默认配置并保存
            save_config(default_data)
            return default_data
    except Exception as e:
        print(f"加载配置文件失败: {e}")
        return default_data

# 保存配置
def save_config(config_data):
    try:
        with open(CONFIG_FILE, 'w', encoding='utf-8') as f:
            json.dump(config_data, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"保存配置文件失败: {e}")

# 加载初始配置
data = load_config()
authorization_codes = data["authorization_codes"]
user_usage = data["user_usage"]
api_call_count = data["api_call_count"]

@app.route('/api/v1/authorize', methods=['POST'])
def authorize():
    """激活授权码"""
    global authorization_codes, user_usage
    
    req_data = request.json
    auth_code = req_data.get('auth_code')
    device_id = req_data.get('device_id')
    
    if not auth_code or not device_id:
        return jsonify({"success": False, "message": "缺少授权码或设备ID"}), 400
    
    # 验证授权码
    if auth_code not in authorization_codes:
        return jsonify({"success": False, "message": "无效的授权码"}), 400
    
    auth_info = authorization_codes[auth_code]
    
    # 检查授权码是否有效
    if not auth_info["active"]:
        return jsonify({"success": False, "message": "授权码已被禁用"}), 400
    
    # 检查授权码是否过期
    if "expiry" in auth_info and auth_info["expiry"] and datetime.fromisoformat(auth_info["expiry"]) < datetime.now():
        return jsonify({"success": False, "message": "授权码已过期"}), 400
    
    # 根据授权码类型处理
    auth_type = auth_info.get("type", "device")  # 默认为设备限制型
    
    if auth_type == "balance":
        # 余额式授权码
        used = auth_info.get("used", 0)
        remaining = auth_info["max_usage"] - used
        
        if remaining <= 0:
            return jsonify({"success": False, "message": "授权码余额已用完"}), 400
            
        # 返回当前余额
        return jsonify({
            "success": True,
            "auth_code": auth_code,
            "max_usage": auth_info["max_usage"],
            "used": used,
            "remaining": remaining,
            "type": "balance"
        })
    else:
        # 设备限制型授权码
        user_key = f"{auth_code}:{device_id}"
        if user_key not in user_usage:
            user_usage[user_key] = {
                "usage_count": 0,
                "last_used": datetime.now().isoformat(),
                "auth_code": auth_code,
                "device_id": device_id
            }
        
        # 返回当前使用情况
        usage_info = user_usage[user_key]
        remaining = auth_info["max_usage"] - usage_info["usage_count"]
        
        return jsonify({
            "success": True,
            "auth_code": auth_code,
            "max_usage": auth_info["max_usage"],
            "usage_count": usage_info["usage_count"],
            "remaining": remaining,
            "type": "device"
        })

@app.route('/api/v1/usage', methods=['GET'])
def get_usage():
    """获取使用情况"""
    auth_code = request.args.get('auth_code')
    device_id = request.args.get('device_id')
    
    if not auth_code or not device_id:
        return jsonify({"success": False, "message": "缺少授权码或设备ID"}), 400
    
    # 验证授权码
    if auth_code not in authorization_codes:
        return jsonify({"success": False, "message": "无效的授权码"}), 400
    
    auth_info = authorization_codes[auth_code]
    
    # 根据授权码类型处理
    auth_type = auth_info.get("type", "device")  # 默认为设备限制型
    
    if auth_type == "balance":
        # 余额式授权码
        used = auth_info.get("used", 0)
        remaining = auth_info["max_usage"] - used
        
        return jsonify({
            "success": True,
            "auth_code": auth_code,
            "max_usage": auth_info["max_usage"],
            "used": used,
            "remaining": remaining,
            "type": "balance"
        })
    else:
        # 设备限制型授权码
        user_key = f"{auth_code}:{device_id}"
        
        if user_key not in user_usage:
            return jsonify({
                "success": True,
                "auth_code": auth_code,
                "max_usage": auth_info["max_usage"],
                "usage_count": 0,
                "remaining": auth_info["max_usage"],
                "type": "device"
            })
        
        # 返回当前使用情况
        usage_info = user_usage[user_key]
        remaining = auth_info["max_usage"] - usage_info["usage_count"]
        
        return jsonify({
            "success": True,
            "auth_code": auth_code,
            "max_usage": auth_info["max_usage"],
            "usage_count": usage_info["usage_count"],
            "remaining": remaining,
            "type": "device"
        })

@app.route('/api/v1/complete', methods=['POST'])
def complete():
    """调用 KIMI API 完成请求"""
    global authorization_codes, user_usage, api_call_count
    
    req_data = request.json
    auth_code = req_data.get('auth_code')
    device_id = req_data.get('device_id')
    model = req_data.get('model', 'moonshot-v1-8k')
    messages = req_data.get('messages', [])
    temperature = req_data.get('temperature', 0.7)
    
    if not auth_code or not device_id or not messages:
        return jsonify({"success": False, "message": "缺少必要参数"}), 400
    
    # 验证授权码
    if auth_code not in authorization_codes:
        return jsonify({"success": False, "message": "无效的授权码"}), 400
    
    auth_info = authorization_codes[auth_code]
    
    # 检查授权码是否有效
    if not auth_info["active"]:
        return jsonify({"success": False, "message": "授权码已被禁用"}), 400
    
    # 检查授权码是否过期
    if "expiry" in auth_info and auth_info["expiry"] and datetime.fromisoformat(auth_info["expiry"]) < datetime.now():
        return jsonify({"success": False, "message": "授权码已过期"}), 400
    
    # 计算使用成本
    usage_cost = 1
    if model == "moonshot-v1-32k":  # 进阶模型
        usage_cost = 2  # 进阶模型消耗2次使用额度
    
    # 根据授权码类型处理
    auth_type = auth_info.get("type", "device")  # 默认为设备限制型
    
    if auth_type == "balance":
        # 余额式授权码
        used = auth_info.get("used", 0)
        remaining = auth_info["max_usage"] - used
        
        if remaining < usage_cost:
            return jsonify({"success": False, "message": f"授权码余额不足，需要{usage_cost}点额度"}), 403
    else:
        # 设备限制型授权码
        user_key = f"{auth_code}:{device_id}"
        if user_key not in user_usage:
            user_usage[user_key] = {
                "usage_count": 0,
                "last_used": datetime.now().isoformat(),
                "auth_code": auth_code,
                "device_id": device_id
            }
        
        usage_info = user_usage[user_key]
        remaining = auth_info["max_usage"] - usage_info["usage_count"]
        
        if remaining < usage_cost:
            return jsonify({"success": False, "message": f"使用次数不足，需要{usage_cost}点额度"}), 403
    
    # 使用管理员API密钥调用KIMI API
    api_url = "https://api.moonshot.cn/v1/chat/completions"
    headers = {
        "Authorization": f"Bearer {REAL_API_KEY}",
        "Content-Type": "application/json"
    }
    payload = {
        "model": model,
        "messages": messages,
        "temperature": temperature,
        "stream": False
    }
    
    try:
        response = requests.post(api_url, headers=headers, json=payload)
        response.raise_for_status()
        result = response.json()
        
        # 更新使用记录
        if auth_type == "balance":
            # 更新余额式授权码的使用次数
            authorization_codes[auth_code]["used"] = authorization_codes[auth_code].get("used", 0) + usage_cost
            new_remaining = auth_info["max_usage"] - authorization_codes[auth_code]["used"]
        else:
            # 更新设备限制型授权码的使用次数
            user_usage[user_key]["usage_count"] += usage_cost
            user_usage[user_key]["last_used"] = datetime.now().isoformat()
            new_remaining = auth_info["max_usage"] - user_usage[user_key]["usage_count"]
        
        api_call_count += 1
        
        # 保存配置
        save_config({
            "authorization_codes": authorization_codes,
            "user_usage": user_usage,
            "api_call_count": api_call_count
        })
        
        # 返回API响应和剩余使用次数
        return jsonify({
            "success": True,
            "response": result,
            "remaining": new_remaining,
            "type": auth_type
        })
    
    except Exception as e:
        return jsonify({"success": False, "message": f"API调用失败: {str(e)}"}), 500

@app.route('/api/v1/admin/generate_code', methods=['POST'])
def generate_code():
    """生成新的授权码（需要管理员权限）"""
    global authorization_codes
    
    req_data = request.json
    admin_key = req_data.get('admin_key')
    max_usage = req_data.get('max_usage', 20)
    days_valid = req_data.get('days_valid')  # 有效天数，None表示永久有效
    code_type = req_data.get('type', 'device')  # 授权码类型，默认为设备限制型
    
    # 验证管理员密钥（实际应用中应使用更安全的方式）
    if admin_key != "yuyan_admin_key":
        return jsonify({"success": False, "message": "无效的管理员密钥"}), 403
    
    # 生成新授权码
    prefix = "YUYAN-BAL" if code_type == "balance" else "YUYAN"
    new_code = f"{prefix}-{uuid.uuid4().hex[:8].upper()}"
    
    # 计算过期时间
    expiry = None
    if days_valid:
        expiry = (datetime.now() + timedelta(days=days_valid)).isoformat()
    
    # 添加到授权码列表
    if code_type == "balance":
        authorization_codes[new_code] = {
            "max_usage": max_usage,
            "used": 0,
            "expiry": expiry,
            "active": True,
            "type": "balance"
        }
    else:
        authorization_codes[new_code] = {
            "max_usage": max_usage,
            "expiry": expiry,
            "active": True,
            "type": "device"
        }
    
    # 保存配置
    save_config({
        "authorization_codes": authorization_codes,
        "user_usage": user_usage,
        "api_call_count": api_call_count
    })
    
    return jsonify({
        "success": True,
        "auth_code": new_code,
        "max_usage": max_usage,
        "expiry": expiry,
        "type": code_type
    })

@app.route('/api/v1/status', methods=['GET'])
def status():
    """获取服务状态"""
    return jsonify({
        "status": "running",
        "api_calls": api_call_count,
        "active_auth_codes": len(authorization_codes),
        "active_users": len(user_usage)
    })

@app.route('/api/v1/admin/auth_codes', methods=['GET'])
def list_auth_codes():
    """获取所有授权码信息（需要管理员权限）"""
    admin_key = request.args.get('admin_key')
    
    # 验证管理员密钥
    if admin_key != "yuyan_admin_key":
        return jsonify({"success": False, "message": "无效的管理员密钥"}), 403
    
    # 准备授权码信息
    auth_code_info = {}
    for code, info in authorization_codes.items():
        # 复制基本信息
        code_info = {
            "max_usage": info["max_usage"],
            "type": info.get("type", "device"),
            "active": info["active"],
            "expiry": info["expiry"]
        }
        
        # 根据类型添加不同的使用信息
        if info.get("type") == "balance":
            code_info["used"] = info.get("used", 0)
            code_info["remaining"] = info["max_usage"] - info.get("used", 0)
        else:
            # 设备限制型，查找所有使用该授权码的设备
            devices = []
            total_used = 0
            for user_key, usage in user_usage.items():
                if usage["auth_code"] == code:
                    device_info = {
                        "device_id": usage["device_id"],
                        "usage_count": usage["usage_count"],
                        "last_used": usage["last_used"]
                    }
                    devices.append(device_info)
                    total_used += usage["usage_count"]
            
            code_info["devices"] = devices
            code_info["total_used"] = total_used
        
        auth_code_info[code] = code_info
    
    return jsonify({
        "success": True,
        "auth_codes": auth_code_info
    })

@app.route('/api/v1/admin/dashboard', methods=['GET'])
def admin_dashboard():
    """简单的管理员仪表板HTML页面"""
    admin_key = request.args.get('admin_key')
    
    # 验证管理员密钥
    if admin_key != "yuyan_admin_key":
        return "未授权访问", 403
    
    # 构建简单的HTML页面
    html = """
    <!DOCTYPE html>
    <html>
    <head>
        <title>语言输入法API管理仪表板</title>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>
            body { font-family: Arial, sans-serif; margin: 20px; }
            h1 { color: #333; }
            table { border-collapse: collapse; width: 100%; margin-top: 20px; }
            th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
            th { background-color: #f2f2f2; }
            tr:nth-child(even) { background-color: #f9f9f9; }
            .success { color: green; }
            .error { color: red; }
            .card { background: #fff; border-radius: 5px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); 
                   padding: 20px; margin-bottom: 20px; }
            .device-list { margin-left: 20px; margin-top: 10px; }
            button { background: #4CAF50; color: white; border: none; padding: 8px 16px; 
                    border-radius: 4px; cursor: pointer; }
            button:hover { background: #45a049; }
            input[type=text], input[type=number] { padding: 8px; width: 100%; margin: 5px 0 15px 0; 
                                                  box-sizing: border-box; }
        </style>
    </head>
    <body>
        <h1>语言输入法API管理仪表板</h1>
        
        <div class="card">
            <h2>生成新授权码</h2>
            <form id="generateForm">
                <label for="maxUsage">最大使用次数:</label>
                <input type="number" id="maxUsage" name="max_usage" value="20" min="1">
                
                <label for="daysValid">有效天数(留空永久有效):</label>
                <input type="number" id="daysValid" name="days_valid" placeholder="输入天数">
                
                <label for="codeType">授权码类型:</label>
                <select id="codeType" name="type">
                    <option value="device">设备限制型</option>
                    <option value="balance">余额式</option>
                </select>
                
                <input type="hidden" name="admin_key" value="yuyan_admin_key">
                <button type="button" onclick="generateCode()">生成授权码</button>
            </form>
            <div id="generateResult"></div>
        </div>
        
        <div class="card">
            <h2>授权码列表</h2>
            <button onclick="refreshAuthCodes()">刷新</button>
            <div id="authCodesList"></div>
        </div>
        
        <div class="card">
            <h2>系统统计</h2>
            <div id="stats"></div>
        </div>
        
        <script>
            // 页面加载时获取数据
            window.onload = function() {
                refreshAuthCodes();
                refreshStats();
            };
            
            // 获取授权码列表
            function refreshAuthCodes() {
                fetch('/api/v1/admin/auth_codes?admin_key=yuyan_admin_key')
                    .then(response => response.json())
                    .then(data => {
                        if (data.success) {
                            displayAuthCodes(data.auth_codes);
                        } else {
                            document.getElementById('authCodesList').innerHTML = 
                                '<p class="error">加载失败: ' + data.message + '</p>';
                        }
                    })
                    .catch(error => {
                        document.getElementById('authCodesList').innerHTML = 
                            '<p class="error">加载失败: ' + error + '</p>';
                    });
            }
            
            // 显示授权码列表
            function displayAuthCodes(authCodes) {
                let html = '<table>';
                html += '<tr><th>授权码</th><th>类型</th><th>状态</th><th>最大使用次数</th>' + 
                        '<th>已用次数</th><th>剩余次数</th><th>过期时间</th><th>设备数</th></tr>';
                
                for (const [code, info] of Object.entries(authCodes)) {
                    const type = info.type === 'balance' ? '余额式' : '设备限制型';
                    const status = info.active ? '<span class="success">有效</span>' : 
                                                '<span class="error">禁用</span>';
                    const expiry = info.expiry ? new Date(info.expiry).toLocaleString() : '永久有效';
                    
                    let used = 0;
                    let remaining = 0;
                    let deviceCount = 0;
                    
                    if (info.type === 'balance') {
                        used = info.used || 0;
                        remaining = info.remaining || 0;
                        deviceCount = '-';
                    } else {
                        used = info.total_used || 0;
                        remaining = info.max_usage - used;
                        deviceCount = (info.devices || []).length;
                    }
                    
                    html += `<tr>
                        <td>${code}</td>
                        <td>${type}</td>
                        <td>${status}</td>
                        <td>${info.max_usage}</td>
                        <td>${used}</td>
                        <td>${remaining}</td>
                        <td>${expiry}</td>
                        <td>${deviceCount}</td>
                    </tr>`;
                    
                    // 如果是设备限制型，显示设备列表
                    if (info.type !== 'balance' && info.devices && info.devices.length > 0) {
                        html += `<tr><td colspan="8">
                            <div class="device-list">
                                <h4>设备列表:</h4>
                                <table style="width: 95%; margin-left: 20px;">
                                    <tr>
                                        <th>设备ID</th>
                                        <th>使用次数</th>
                                        <th>最后使用时间</th>
                                    </tr>`;
                        
                        for (const device of info.devices) {
                            const lastUsed = new Date(device.last_used).toLocaleString();
                            html += `<tr>
                                <td>${device.device_id}</td>
                                <td>${device.usage_count}</td>
                                <td>${lastUsed}</td>
                            </tr>`;
                        }
                        
                        html += `</table></div></td></tr>`;
                    }
                }
                
                html += '</table>';
                document.getElementById('authCodesList').innerHTML = html;
            }
            
            // 生成新授权码
            function generateCode() {
                const form = document.getElementById('generateForm');
                const formData = new FormData(form);
                const data = {};
                
                for (const [key, value] of formData.entries()) {
                    if (value) {
                        data[key] = value;
                    }
                }
                
                fetch('/api/v1/admin/generate_code', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(data),
                })
                .then(response => response.json())
                .then(data => {
                    if (data.success) {
                        document.getElementById('generateResult').innerHTML = 
                            `<p class="success">生成成功: ${data.auth_code}</p>`;
                        refreshAuthCodes();
                    } else {
                        document.getElementById('generateResult').innerHTML = 
                            `<p class="error">生成失败: ${data.message}</p>`;
                    }
                })
                .catch(error => {
                    document.getElementById('generateResult').innerHTML = 
                        `<p class="error">生成失败: ${error}</p>`;
                });
            }
            
            // 获取系统统计信息
            function refreshStats() {
                fetch('/api/v1/status')
                    .then(response => response.json())
                    .then(data => {
                        let html = `
                            <p>服务状态: <span class="success">${data.status}</span></p>
                            <p>API调用次数: ${data.api_calls}</p>
                            <p>激活授权码数量: ${data.active_auth_codes}</p>
                            <p>活跃用户数: ${data.active_users}</p>
                        `;
                        document.getElementById('stats').innerHTML = html;
                    })
                    .catch(error => {
                        document.getElementById('stats').innerHTML = 
                            `<p class="error">加载失败: ${error}</p>`;
                    });
            }
        </script>
    </body>
    </html>
    """
    
    return html

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=7999, debug=True)