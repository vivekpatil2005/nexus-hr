async function verifySignup() {
  try {
    console.log('=== Starting Self-Registration (Signup) Flow Verification ===');

    const randomNum = Math.floor(Math.random() * 100000);
    const username = `signupuser.${randomNum}`;
    const email = `signup.${randomNum}@example.com`;
    const fullName = `Signup User ${randomNum}`;
    const password = `SecureSignupPass2026!`;

    console.log(`\n[1/3] Registering new account: username=${username}, email=${email}, name="${fullName}"...`);

    const signupRes = await fetch('http://localhost:8080/api/auth/signup', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username,
        email,
        fullName,
        password
      })
    });

    console.log(`Signup response status: ${signupRes.status}`);
    if (signupRes.status !== 201) {
      const err = await signupRes.json();
      throw new Error(`Signup failed with code ${signupRes.status}: ${JSON.stringify(err)}`);
    }

    const signupData = await signupRes.json();
    console.log('✔ Signup successful. Server returned response body.');

    const { accessToken, refreshToken, username: returnedUsername, roles } = signupData.data;
    console.log(`✔ Received JWT Tokens:`);
    console.log(`  - Access Token (first 20 chars): ${accessToken.substring(0, 20)}...`);
    console.log(`  - Refresh Token (first 20 chars): ${refreshToken.substring(0, 20)}...`);
    console.log(`  - Returned Username: ${returnedUsername}`);
    console.log(`  - Assigned Roles: ${JSON.stringify(roles)}`);

    if (returnedUsername !== username) {
      throw new Error(`Username mismatch: Sent "${username}" but received "${returnedUsername}"`);
    }

    // 2. Fetch profile info using the new access token
    console.log('\n[2/3] Fetching profile information via "/api/auth/me" using new access token...');
    const profileRes = await fetch('http://localhost:8080/api/auth/me', {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${accessToken}`
      }
    });

    if (!profileRes.ok) {
      throw new Error(`Profile fetch failed with status: ${profileRes.status}`);
    }

    const profileData = await profileRes.json();
    const profile = profileData.data;
    console.log('✔ Profile details fetched successfully:');
    console.log(`  - ID: ${profile.id}`);
    console.log(`  - Full Name: ${profile.fullName}`);
    console.log(`  - Email: ${profile.email}`);
    console.log(`  - Roles: ${JSON.stringify(profile.roles)}`);
    console.log(`  - Enabled Status: ${profile.enabled}`);

    if (profile.fullName !== fullName || profile.email !== email) {
      throw new Error('Profile details verification failed: Mismatch on email or full name.');
    }

    // 3. Confirm login endpoint works with these newly created credentials
    console.log('\n[3/3] Authenticating credentials at "/api/auth/login" to confirm persistence...');
    const loginRes = await fetch('http://localhost:8080/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        usernameOrEmail: username,
        password: password
      })
    });

    if (!loginRes.ok) {
      throw new Error(`Login failed with status: ${loginRes.status}`);
    }
    console.log('✔ Login credentials verified and accepted.');

    console.log('\n=============================================================');
    console.log('🎉 SIGNUP FLOW VERIFICATION SUCCESS: Self-registration works perfectly!');
    console.log('=============================================================');

  } catch (error) {
    console.error('\n❌ SIGNUP FLOW VERIFICATION FAILURE:', error.message);
    process.exit(1);
  }
}

verifySignup();
