/**
 * this.PROJECT_ID
 */
before(() => {
  cy.login()

  cy.fetch({ url: '/api/v1/projects/', log: false })
    .then(({ body: { results } }) => {
      const { id } = results.find(
        ({ name }) => name.toLowerCase() === 'cypress',
      )

      return id
    })
    .as('PROJECT_ID')

  cy.logout()
})
