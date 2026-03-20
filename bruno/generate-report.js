
const fs = require('fs');
const rawData = JSON.parse(fs.readFileSync('results_all_fixed.json', 'utf8'));

// If it's an array of iteration results
const allResults = rawData[0].results;

let totalRequests = allResults.length;
let passedRequests = 0;
let failedRequests = 0;
let totalTests = 0;
let passedTests = 0;
let failedTests = 0;

let report = '# Mawrid API Test Suite Report\n\n';
report += 'Date: ' + new Date().toLocaleDateString() + '\n\n';

let tableRows = '';

allResults.forEach(res => {
  const folder = res.test.filename.split(/[\\/]/).slice(0, -1).join('/') || 'Root';
  const name = res.test.filename;
  const status = res.response.status;
  
  let suitePassed = true;
  let failMessages = [];
  
  if (res.testResults) {
    res.testResults.forEach(tr => {
      totalTests++;
      if (tr.status === 'fail') {
        suitePassed = false;
        failedTests++;
        failMessages.push(tr.message);
      } else {
        passedTests++;
      }
    });
  }

  if (suitePassed) passedRequests++;
  else failedRequests++;

  const resultStr = suitePassed ? '✅ PASS' : '❌ FAIL';
  const explanation = suitePassed ? 'As expected' : failMessages.join('; ');
  
  tableRows += `| ${folder} | ${name} | ${status} | ${resultStr} | ${explanation} |\n`;
});

report += '## Execution Summary\n';
report += `- **Total Requests:** ${totalRequests}\n`;
report += `- **Passed Requests:** ${passedRequests}\n`;
report += `- **Failed Requests:** ${failedRequests}\n`;
report += `- **Total Tests:** ${totalTests}\n`;
report += `- **Passed Tests:** ${passedTests}\n`;
report += `- **Failed Tests:** ${failedTests}\n\n`;

report += '## Detailed Results\n\n';
report += '| Category | Test Name | Status | Result | Explanation |\n';
report += '|----------|-----------|--------|--------|-------------|\n';
report += tableRows;

report += '\n## Failure Analysis & Bug List\n\n';
report += '### Urgent Bugs\n';
report += '- **Non-Leaf Category Creation (Bug):** `edge_cases/demande_creation/07-non-leaf-category` returned **201**. The system allowed creating a demande for a parent category (ID 1). This violates business rules as demandes should only be in leaf categories.\n';
report += '- **Duplicate Status Code Ambiguity:** `edge_cases/demande_creation/11-create-no-token` returned **403** instead of **401**. This is misleading as it should indicate lack of authentication, not lack of permission.\n';

report += '\n### Minor Inconsistencies & Boundary Issues\n';
report += '- **Negative Pagination Size:** `edge_cases/pagination/03-size-negative` returned **200**. The server accepted `size=-1` instead of returning a 400 Validation Error. This can lead to unexpected database query behavior.\n';
report += '- **Authentication Status Codes:** Logins with wrong passwords return **401** instead of the documented **400**. README says 400 for invalid credentials.\n';
report += '- **Non-Existent Category:** `edge_cases/demande_creation/06-non-existent-category` returned **404**. While technically accurate, it is better to return 400 when an input ID is invalid but the endpoint exists.\n';
report += '- **Fake Token Handling:** Using a completely fake JWT results in **403** (Forbidden) instead of **401** (Unauthorized).\n';

report += '\n### Conclusion\n';
report += 'The edge case suite has identified critical business logic gaps (non-leaf categories) and several security-related status code inconsistencies. The system is mostly robust but requires better input validation for pagination and category depth checks.\n';

fs.writeFileSync('FINAL_TEST_REPORT.md', report);
console.log('Report generated: FINAL_TEST_REPORT.md');
