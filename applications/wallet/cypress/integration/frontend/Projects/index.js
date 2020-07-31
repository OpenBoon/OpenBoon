describe('Projects', function () {
  it('should have a dashboard', function () {
    cy.login()

    cy.visit('/')

    cy.contains('Go To Project').click()

    cy.contains('Project Dashboard')
  })
})
