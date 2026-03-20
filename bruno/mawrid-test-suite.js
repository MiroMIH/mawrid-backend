
const BASE_URL = 'http://localhost:8080/api/v1';

const BUYER = { email: 'buyer@test.com', password: 'Password1!' };
const BUYER2 = { email: 'buyer2@test.com', password: 'Password1!' };
const SUPPLIER = { email: 'supplier@test.com', password: 'Password1!' };
const SUPPLIER2 = { email: 'supplier2@test.com', password: 'Password1!' };
const ADMIN = { email: 'superadmin@mawrid.dz', password: 'SuperAdmin@2026' };

const results = [];

async function api(path, method = 'GET', body = null, token = null) {
    const options = {
        method,
        headers: {
            'Content-Type': 'application/json',
        }
    };
    if (token) options.headers['Authorization'] = `Bearer ${token}`;
    if (body) options.body = JSON.stringify(body);

    const start = Date.now();
    try {
        const response = await fetch(`${BASE_URL}${path}`, options);
        const duration = Date.now() - start;
        let data = null;
        try {
            data = await response.json();
        } catch (e) {}
        return { status: response.status, data, duration };
    } catch (error) {
        return { status: 500, data: { message: error.message }, duration: 0 };
    }
}

function logTest(name, response, expectedStatus) {
    const passed = response.status === expectedStatus;
    results.push({
        name,
        status: response.status,
        passed,
        actualResponse: response.data,
        expectedStatus
    });
    console.log(`[${passed ? 'PASS' : 'FAIL'}] ${name} (Status: ${response.status})`);
}

async function runTests() {
    console.log('--- STARTING MAWRID TEST SUITE ---');

    // 1. AUTHENTICATION
    let buyerToken, buyer2Token, supplierToken, supplier2Token, adminToken;

    console.log('\n--- AUTHENTICATION EDGE CASES ---');
    logTest('Login with wrong password', await api('/auth/login', 'POST', { email: BUYER.email, password: 'WrongPassword' }), 400);
    logTest('Login with non-existent email', await api('/auth/login', 'POST', { email: 'nonexistent@test.com', password: 'Password1!' }), 400);
    logTest('Login with empty credentials', await api('/auth/login', 'POST', { email: '', password: '' }), 400);

    const buyerLogin = await api('/auth/login', 'POST', BUYER);
    buyerToken = buyerLogin.data.accessToken;
    logTest('Login as Buyer', buyerLogin, 200);

    const buyer2Login = await api('/auth/login', 'POST', BUYER2);
    buyer2Token = buyer2Login.data.accessToken;
    logTest('Login as Buyer 2', buyer2Login, 200);

    const supplierLogin = await api('/auth/login', 'POST', SUPPLIER);
    supplierToken = supplierLogin.data.accessToken;
    logTest('Login as Supplier', supplierLogin, 200);

    const supplier2Login = await api('/auth/login', 'POST', SUPPLIER2);
    supplier2Token = supplier2Login.data.accessToken;
    logTest('Login as Supplier 2', supplier2Login, 200);

    const adminLogin = await api('/auth/login', 'POST', ADMIN);
    adminToken = adminLogin.data.accessToken;
    logTest('Login as Admin', adminLogin, 200);

    logTest('Use fake token', await api('/users/me', 'GET', null, 'fake.token.here'), 401);
    logTest('Buyer access supplier route', await api('/seller/feed', 'GET', null, buyerToken), 403);
    logTest('Supplier access buyer route', await api('/buyer/demandes', 'GET', null, supplierToken), 403);

    // 2. DEMANDE CREATION
    console.log('\n--- DEMANDE CREATION EDGE CASES ---');
    const pastDate = new Date(); pastDate.setDate(pastDate.getDate() - 5);
    const todayDate = new Date();
    
    logTest('Create with past deadline', await api('/buyer/demandes', 'POST', { title: 'Past', quantity: 10, categoryId: 12, deadline: pastDate.toISOString().split('T')[0] }, buyerToken), 400);
    logTest('Create with quantity 0', await api('/buyer/demandes', 'POST', { title: 'Zero', quantity: 0, categoryId: 12, deadline: '2026-12-31' }, buyerToken), 400);
    logTest('Create with quantity -5', await api('/buyer/demandes', 'POST', { title: 'Neg', quantity: -5, categoryId: 12, deadline: '2026-12-31' }, buyerToken), 400);
    logTest('Create with no title', await api('/buyer/demandes', 'POST', { quantity: 10, categoryId: 12, deadline: '2026-12-31' }, buyerToken), 400);
    logTest('Create with space title', await api('/buyer/demandes', 'POST', { title: '   ', quantity: 10, categoryId: 12, deadline: '2026-12-31' }, buyerToken), 400);
    logTest('Create with non-existent category', await api('/buyer/demandes', 'POST', { title: 'No Cat', quantity: 10, categoryId: 9999, deadline: '2026-12-31' }, buyerToken), 400);
    logTest('Create with non-leaf category', await api('/buyer/demandes', 'POST', { title: 'Non-Leaf', quantity: 10, categoryId: 1, deadline: '2026-12-31' }, buyerToken), 400);
    logTest('Create with long title', await api('/buyer/demandes', 'POST', { title: 'A'.repeat(501), quantity: 10, categoryId: 12, deadline: '2026-12-31' }, buyerToken), 400);
    logTest('Create as supplier (blocked)', await api('/buyer/demandes', 'POST', { title: 'Supplier Try', quantity: 10, categoryId: 12, deadline: '2026-12-31' }, supplierToken), 403);
    logTest('Create with no token (blocked)', await api('/buyer/demandes', 'POST', { title: 'No Token', quantity: 10, categoryId: 12, deadline: '2026-12-31' }), 401);

    const validDemande = await api('/buyer/demandes', 'POST', { title: 'Valid Demande', quantity: 100, categoryId: 12, deadline: '2026-12-31', wilaya: '16' }, buyerToken);
    const demandeId = validDemande.data?.data?.id;
    logTest('Create valid demande', validDemande, 201);

    // 3. STATUS TRANSITION
    console.log('\n--- DEMANDE STATUS TRANSITION EDGE CASES ---');
    if (demandeId) {
        logTest('Close owned demande', await api(`/buyer/demandes/${demandeId}/close`, 'PATCH', null, buyerToken), 200);
        logTest('Close already closed demande', await api(`/buyer/demandes/${demandeId}/close`, 'PATCH', null, buyerToken), 400);
        logTest('Cancel closed demande', await api(`/buyer/demandes/${demandeId}`, 'DELETE', null, buyerToken), 400);
        
        const d2 = await api('/buyer/demandes', 'POST', { title: 'D2 for Cancel', quantity: 10, categoryId: 12, deadline: '2026-12-31', wilaya: '16' }, buyerToken);
        const d2Id = d2.data?.data?.id;
        logTest('Cancel other buyer demande', await api(`/buyer/demandes/${d2Id}`, 'DELETE', null, buyer2Token), 403);
        logTest('Close other buyer demande', await api(`/buyer/demandes/${d2Id}/close`, 'PATCH', null, buyer2Token), 403);
        
        // Test cancel with DISPONIBLE response
        await api('/seller/categories', 'PATCH', { categoryIds: [12] }, supplierToken);
        // Wait a bit for matching engine
        await new Promise(r => setTimeout(r, 200));
        await api(`/seller/reponses/${d2Id}`, 'POST', { status: 'DISPONIBLE', note: 'Ready' }, supplierToken);
        logTest('Cancel with DISPONIBLE response', await api(`/buyer/demandes/${d2Id}`, 'DELETE', null, buyerToken), 409);
    }

    // 4. FEED & RESPONSES
    console.log('\n--- FEED & RESPONSES EDGE CASES ---');
    logTest('Get feed as buyer', await api('/seller/feed', 'GET', null, buyerToken), 403);
    logTest('Get feed no token', await api('/seller/feed', 'GET'), 401);
    
    await api('/seller/categories', 'PATCH', { categoryIds: [12] }, supplierToken);
    const d3 = await api('/buyer/demandes', 'POST', { title: 'Feed Test', quantity: 10, categoryId: 12, deadline: '2026-12-31', wilaya: '16' }, buyerToken);
    const d3Id = d3.data?.data?.id;
    
    logTest('Submit response to own match', await api(`/seller/reponses/${d3Id}`, 'POST', { status: 'DISPONIBLE' }, supplierToken), 201);
    logTest('Submit duplicate response', await api(`/seller/reponses/${d3Id}`, 'POST', { status: 'DISPONIBLE' }, supplierToken), 409);
    logTest('Submit response no status', await api(`/seller/reponses/${d3Id}`, 'POST', {}, supplierToken), 400);
    logTest('Submit response invalid status', await api(`/seller/reponses/${d3Id}`, 'POST', { status: 'MAYBE' }, supplierToken), 400);
    
    await api(`/buyer/demandes/${d3Id}/close`, 'PATCH', null, buyerToken);
    logTest('Submit response to closed demande', await api(`/seller/reponses/${d3Id}`, 'POST', { status: 'DISPONIBLE' }, supplier2Token), 400);

    // 5. ADMIN
    console.log('\n--- ADMIN EDGE CASES ---');
    logTest('Force close as buyer', await api(`/admin/demandes/${d3Id}/force-close`, 'PATCH', null, buyerToken), 403);
    logTest('Force close non-existent', await api(`/admin/demandes/00000000-0000-0000-0000-000000000000/force-close`, 'PATCH', null, adminToken), 404);
    logTest('Recategorize to non-existent', await api(`/admin/demandes/${d3Id}/recategorize`, 'PATCH', { newCategoryId: 9999 }, adminToken), 400);
    
    // 6. SECURITY
    console.log('\n--- SECURITY & AUTHORIZATION ---');
    logTest('Buyer 1 view Buyer 2 demande', await api(`/buyer/demandes/${d3Id}`, 'GET', null, buyer2Token), 403);
    
    // 7. PAGINATION
    console.log('\n--- PAGINATION & FILTERS ---');
    logTest('Page 0 size 1', await api('/buyer/demandes?page=0&size=1', 'GET', null, buyerToken), 200);
    logTest('Page 999 empty', await api('/buyer/demandes?page=999', 'GET', null, buyerToken), 200);
    logTest('Size -1 handle', await api('/buyer/demandes?size=-1', 'GET', null, buyerToken), 400);

    console.log('\n--- TESTS COMPLETE ---');
    generateReport();
}

function generateReport() {
    const passed = results.filter(r => r.passed).length;
    const failed = results.filter(r => !r.passed).length;

    let md = `# Mawrid Test Results\n\n`;
    md += `**Total Passed:** ${passed}\n`;
    md += `**Total Failed:** ${failed}\n\n`;
    md += `## Detailed Results\n\n`;
    md += `| Test | Status | Result | Explanation |\n`;
    md += `|------|--------|--------|-------------|\n`;

    results.forEach(r => {
        let explanation = '';
        if (!r.passed) {
            explanation = r.actualResponse?.message || JSON.stringify(r.actualResponse);
        } else {
            explanation = 'As expected';
        }
        md += `| ${r.name} | ${r.status} | ${r.passed ? '✅ PASS' : '❌ FAIL'} | ${explanation} |\n`;
    });

    console.log(\`\nTOTAL PASSED: \${passed}\`);
    console.log(\`TOTAL FAILED: \${failed}\`);
    
    require('fs').writeFileSync('DETAILED_TEST_RESULTS.md', md);
    console.log('Report generated: DETAILED_TEST_RESULTS.md');
}

runTests();
