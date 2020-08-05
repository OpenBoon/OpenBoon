describe('Authentication', function () {
  it('can log in and log out.', function () {
    cy.login()
    cy.request('GET', '/api/v1/me/')
    cy.apiRequest({ method: 'POST', url: '/api/v1/logout/', body: {} })
    cy.apiRequest({
      method: 'GET',
      url: '/api/v1/me/',
      okStatusCodes: [403],
    })
  })
})
