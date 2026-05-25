async function runVerification() {
  try {
    // Helper function for Quoted-Printable decoding
    function decodeQP(str) {
      return str
        .replace(/=\r?\n/g, '')
        .replace(/=[0-9A-F]{2}/gi, (match) => String.fromCharCode(parseInt(match.slice(1), 16)));
    }

    console.log('=== Starting End-to-End Onboarding and Settings flow verification ===');

    // 1. Admin logs in to obtain a JWT token
    console.log('\n[1/7] Admin logging in...');
    const adminLoginRes = await fetch('http://localhost:8080/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ usernameOrEmail: 'admin', password: 'admin123' })
    });
    if (!adminLoginRes.ok) {
      throw new Error(`Admin login failed: ${adminLoginRes.status}`);
    }
    const adminLoginData = await adminLoginRes.json();
    const adminToken = adminLoginData.data.accessToken;
    console.log('✔ Admin logged in successfully.');

    // 2. Admin registers a new employee
    const randomNum = Math.floor(Math.random() * 100000);
    const email = `employee.flow.${randomNum}@example.com`;
    const firstName = `FlowUser${randomNum}`;
    const lastName = `Integration`;
    
    console.log(`\n[2/7] Creating new employee: ${firstName} ${lastName} (${email})...`);
    const empRes = await fetch('http://localhost:8080/api/employees', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${adminToken}`
      },
      body: JSON.stringify({
        firstName,
        lastName,
        email,
        phone: '9988776655',
        designation: 'Backend Developer',
        status: 'ACTIVE',
        ctc: 900000,
        hireDate: '2026-05-24'
      })
    });
    if (!empRes.ok) {
      const err = await empRes.json();
      throw new Error(`Employee creation failed: ${JSON.stringify(err)}`);
    }
    const empData = await empRes.json();
    console.log(`✔ Employee created with ID ${empData.data.id} and EmpCode ${empData.data.empCode}.`);

    // 3. Wait for event listener & query MailHog for credentials
    console.log('\n[3/7] Waiting for onboarding event processing & email dispatch...');
    await new Promise((resolve) => setTimeout(resolve, 3000));

    console.log('Querying MailHog...');
    const mailRes = await fetch('http://localhost:8025/api/v2/messages');
    if (!mailRes.ok) {
      throw new Error(`MailHog query failed: ${mailRes.status}`);
    }
    const mailData = await mailRes.json();
    const welcomeMail = mailData.items.find(item => 
      item.Content.Headers.Subject[0].includes('Welcome to NexusHR') &&
      item.Content.Headers.To[0].includes(email)
    );
    if (!welcomeMail) {
      throw new Error('Verification failed: Welcome email not found in MailHog.');
    }
    console.log('✔ Welcome email found in MailHog.');

    const body = decodeQP(welcomeMail.Content.Body);
    const usernameMatch = body.match(/Username:<\/strong><\/td>\s*<td[^>]*>\s*([a-zA-Z0-9.]+)\s*<\/td>/i);
    const passwordMatch = body.match(/Temp Password:<\/strong><\/td>\s*<td[^>]*>\s*([a-zA-Z0-9!@#$]+)\s*<\/td>/i);

    if (!usernameMatch || !passwordMatch) {
      throw new Error(`Could not parse credentials from email html body. Body preview:\n${body.substring(0, 500)}`);
    }

    const username = usernameMatch[1].trim();
    const tempPassword = passwordMatch[1].trim();
    console.log(`✔ Parsed credentials from welcome email:`);
    console.log(`  - Username: ${username}`);
    console.log(`  - Temp Password: ${tempPassword}`);

    // 4. Log in as the new employee with temporary password
    console.log('\n[4/7] Logging in as new employee with temporary credentials...');
    const userLoginRes = await fetch('http://localhost:8080/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ usernameOrEmail: username, password: tempPassword })
    });
    if (!userLoginRes.ok) {
      throw new Error(`New user login with temporary password failed with status: ${userLoginRes.status}`);
    }
    const userLoginData = await userLoginRes.json();
    let userToken = userLoginData.data.accessToken;
    console.log('✔ Employee logged in successfully with temporary credentials. Token obtained.');

    // 5. Change the temporary password to a new secure password
    const newPassword = `SecureFlowPass2026!`;
    console.log(`\n[5/7] Changing password from Settings API to: "${newPassword}"...`);
    const changePassRes = await fetch('http://localhost:8080/api/auth/change-password', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${userToken}`
      },
      body: JSON.stringify({
        currentPassword: tempPassword,
        newPassword: newPassword
      })
    });
    if (!changePassRes.ok) {
      const err = await changePassRes.json();
      throw new Error(`Change password failed: ${JSON.stringify(err)}`);
    }
    console.log('✔ Password changed successfully via Settings endpoint.');

    // 6. Verify that logging in with the old temporary password is rejected
    console.log('\n[6/7] Verifying that old temporary password is now rejected...');
    const oldLoginRes = await fetch('http://localhost:8080/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ usernameOrEmail: username, password: tempPassword })
    });
    console.log(`Old password login status code: ${oldLoginRes.status} (Expected: 401)`);
    if (oldLoginRes.status !== 401) {
      throw new Error(`Security violation: Old temporary password was not invalidated! Login returned status: ${oldLoginRes.status}`);
    }
    console.log('✔ Verified: Old temporary password successfully rejected.');

    // 7. Verify login works with the new password
    console.log('\n[7/7] Verifying login with the newly set password...');
    const newLoginRes = await fetch('http://localhost:8080/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ usernameOrEmail: username, password: newPassword })
    });
    if (!newLoginRes.ok) {
      throw new Error(`Login with new password failed with status: ${newLoginRes.status}`);
    }
    const newLoginData = await newLoginRes.json();
    userToken = newLoginData.data.accessToken;
    console.log('✔ Login with new password succeeded!');

    // Get profile details to confirm API works with new token
    const meRes = await fetch('http://localhost:8080/api/auth/me', {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${userToken}`
      }
    });
    const meData = await meRes.json();
    console.log('✔ Fetched "/api/auth/me" profile metadata:');
    console.log(`  - Username: ${meData.data.username}`);
    console.log(`  - Full Name: ${meData.data.fullName}`);
    console.log(`  - Roles: ${JSON.stringify(meData.data.roles)}`);
    console.log(`  - Employee ID Reference: ${meData.data.employeeId}`);

    console.log('\n=============================================================');
    console.log('🎉 E2E VERIFICATION SUCCESS: All phases of the request are working perfectly!');
    console.log('=============================================================');

  } catch (error) {
    console.error('\n❌ VERIFICATION FAILURE:', error.message);
    process.exit(1);
  }
}

runVerification();
