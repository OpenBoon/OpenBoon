describe('Webhooks', function () {
  it('can be created, deactivated, and deleted', function () {
    const webhookUrl = `https://us-central1-zorroa-deploy.cloudfunctions.net/webhook-tester?${Date.now()}`

    cy.login()

    /**
     * Create
     */

    cy.visit(`/${this.PROJECT_ID}/webhooks/add`)

    cy.get('input[name=url]').type(webhookUrl)

    cy.get('input[name=secretKey]').clear().type('secret')

    cy.contains('Asset Modified').click()
    cy.contains('Asset Analyzed').click()

    cy.get('button[type=submit]').contains('Create Webhook').click()

    cy.contains('Webhook created.')

    /**
     * Deactivate
     */

    cy.contains(webhookUrl).parent().siblings().last().click()

    cy.contains('Deactivate').click()

    /**
     * Delete
     */

    cy.contains(webhookUrl).parent().siblings().last().click()

    cy.contains('Delete').click()

    cy.contains('Delete Permanently').click()

    cy.contains('Webhook deleted.')

    cy.contains(webhookUrl).should('not.exist')
  })
})
