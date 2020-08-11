describe('Models', function () {
  it('can be created', function () {
    const modelName = `cypress-frontend-${Date.now()}`

    cy.login()

    cy.visit(`/${this.PROJECT_ID}/models/add`)

    cy.contains('Model Type').get('select').select('ZVI_KNN_CLASSIFIER')

    cy.get('input[name=name]').type(modelName)

    cy.get('button[type=submit]').contains('Create New Model').click()

    cy.contains('Model created.')

    cy.contains(modelName)
  })
})
