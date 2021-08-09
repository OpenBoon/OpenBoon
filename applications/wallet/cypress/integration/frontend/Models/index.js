describe('Models', function () {
  it('can be created and deleted', function () {
    const modelName = `cypress-frontend-${Date.now()}`

    cy.login()

    /**
     * Create
     */

    cy.visit(`/${this.PROJECT_ID}/models/add`)

    cy.get('input[name=name]').type(modelName)

    cy.contains('Face Recognition').click()

    cy.get('button[type=submit]').contains('Create New Model').click()

    cy.contains('Model created.')

    /**
     * Delete
     */

    cy.contains(modelName).click()

    cy.contains('Model Details')

    cy.get('button[aria-label="Toggle Actions Menu"]').click()

    cy.contains('Delete').click()

    cy.contains('Delete Permanently').click()

    cy.url().should(
      'eq',
      `${Cypress.config('baseUrl')}/${this.PROJECT_ID}/models`,
    )

    cy.contains('Model deleted.')

    cy.contains(modelName).should('not.exist')
  })
})
