async function testOnboardingFlow() {
  try {
    // 1. Login as Admin to get JWT token
    const loginResponse = await fetch('http://localhost:8080/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        usernameOrEmail: 'admin',
        password: 'admin123',
      }),
    });
    
    if (!loginResponse.ok) {
      throw new Error(`Login failed with status: ${loginResponse.status}`);
    }
    
    const loginData = await loginResponse.json();
    const token = loginData.data.accessToken;
    console.log('Successfully logged in as Admin. Token obtained.');

    // Generate random email to prevent uniqueness conflicts
    const randomNum = Math.floor(Math.random() * 100000);
    const email = `test.employee${randomNum}@example.com`;
    const firstName = `TestUser${randomNum}`;
    const lastName = `Onboarding`;

    // 2. Create a new employee
    console.log(`Creating new employee: ${firstName} ${lastName} with email ${email}`);
    const employeeResponse = await fetch('http://localhost:8080/api/employees', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        firstName: firstName,
        lastName: lastName,
        email: email,
        phone: '9876543299',
        designation: 'Software Engineer',
        status: 'ACTIVE',
        ctc: 1200000,
        hireDate: '2026-05-24'
      }),
    });

    if (!employeeResponse.ok) {
      const errBody = await employeeResponse.json();
      throw new Error(`Failed to create employee: ${JSON.stringify(errBody)}`);
    }

    const employeeData = await employeeResponse.json();
    console.log('Employee created successfully:', employeeData.data);

    // Wait a couple of seconds for async email sending
    console.log('Waiting for asynchronous email delivery...');
    await new Promise((resolve) => setTimeout(resolve, 3000));

    // 3. Query MailHog API to check for the sent email
    console.log('Querying MailHog messages...');
    const mailResponse = await fetch('http://localhost:8025/api/v2/messages');
    if (!mailResponse.ok) {
      throw new Error(`Failed to query MailHog: ${mailResponse.status}`);
    }

    const mailData = await mailResponse.json();
    console.log(`Total messages in MailHog: ${mailData.total}`);

    const welcomeMail = mailData.items.find(item => 
      item.Content.Headers.Subject[0].includes('Welcome to NexusHR') &&
      item.Content.Headers.To[0].includes(email)
    );

    if (welcomeMail) {
      console.log('SUCCESS: Onboarding email found in MailHog!');
      console.log('Subject:', welcomeMail.Content.Headers.Subject[0]);
      console.log('To:', welcomeMail.Content.Headers.To[0]);
      
      // Decode Quoted-Printable content
      const rawBody = welcomeMail.Content.Body;
      const body = rawBody
        .replace(/=\r?\n/g, '')
        .replace(/=[0-9A-F]{2}/gi, (match) => String.fromCharCode(parseInt(match.slice(1), 16)));

      const usernameMatch = body.match(/Username:<\/strong><\/td>\s*<td[^>]*>\s*([a-zA-Z0-9.]+)\s*<\/td>/i);
      const passwordMatch = body.match(/Temp Password:<\/strong><\/td>\s*<td[^>]*>\s*([a-zA-Z0-9!@#$]+)\s*<\/td>/i);
      
      if (usernameMatch) {
        console.log('Generated Username:', usernameMatch[1].trim());
      } else {
        console.log('Could not parse Username from HTML');
      }
      
      if (passwordMatch) {
        console.log('Generated Temp Password:', passwordMatch[1].trim());
      } else {
        console.log('Could not parse Password from HTML');
      }
      
      console.log('Verification completed successfully!');
    } else {
      console.log('FAILURE: Onboarding email NOT found in MailHog.');
    }

  } catch (error) {
    console.error('Test failed with error:', error.message);
  }
}

testOnboardingFlow();
