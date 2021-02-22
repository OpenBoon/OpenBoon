describe('Visualizer', function () {
  describe('Filters', function () {
    beforeEach(() => {
      /**
       * zooming in displays fewer assets
       * which causes less stress on the network
       * and triggers fewer errors
       */
      localStorage.setItem(
        'Assets',
        JSON.stringify({ columnCount: 1, isMin: false, isMax: true }),
      )
    })

    it('can add a Simple Text filter', function () {
      cy.login()

      cy.visit(`/${this.PROJECT_ID}/visualizer`)

      cy.get('button[aria-label="Filters"]').click()

      cy.get('input[aria-label="Add Simple Text Filter"]').type('Daisy{enter}')

      cy.selectFirstAsset()
    })

    it('can delete a filter', function () {
      cy.login()

      cy.visit(`/${this.PROJECT_ID}/visualizer`)

      cy.get('button[aria-label="Filters"]').click()

      cy.get('input[aria-label="Add Simple Text Filter"]').type('Daisy{enter}')

      cy.get('button[aria-label="Delete Filter"]').click()

      cy.contains('Daisy').should('not.exist')
    })

    it('can clear all filters', function () {
      cy.login()

      cy.visit(`/${this.PROJECT_ID}/visualizer`)

      cy.get('button[aria-label="Filters"]').click()

      cy.get('input[aria-label="Add Simple Text Filter"]').type('Daisy{enter}')

      cy.contains('Clear All Filters').click()

      cy.contains('Daisy').should('not.exist')
    })

    it('can add an Exists filter', function () {
      cy.login()

      cy.visit(`/${this.PROJECT_ID}/visualizer`)

      cy.get('button[aria-label="Filters"]').click()

      cy.contains('Add Filters').click()

      cy.get('summary[aria-label="Source"]').click()

      cy.contains('filesize').click()

      cy.contains('Add Filters').click()

      cy.contains('range').get('select').select('exists')

      cy.contains('Missing').click()

      cy.contains('Show assets missing the field "source.filesize"')

      cy.contains('All assets have been filtered out.')
    })

    it('can add a Range filter', function () {
      cy.login()

      cy.visit(`/${this.PROJECT_ID}/visualizer`)

      cy.get('button[aria-label="Filters"]').click()

      cy.contains('Add Filters').click()

      cy.get('summary[aria-label="Source"]').click()

      cy.contains('filesize').click()

      cy.contains('Add Filters').click()

      cy.contains('source.filesize')

      cy.contains('MIN').click()

      cy.focused().clear().type('10000000').type('{enter}')

      cy.selectFirstAsset()
    })

    it('can add a Facet filter', function () {
      cy.login()

      cy.visit(`/${this.PROJECT_ID}/visualizer`)

      cy.get('button[aria-label="Filters"]').click()

      cy.contains('Add Filters').click()

      cy.get('summary[aria-label="Location"]').click()

      cy.contains('city').click()

      cy.contains('Add Filters').click()

      cy.contains('location.city')

      cy.contains('Long Beach').click()

      cy.selectFirstAsset()
    })

    it('can add a Text Detection filter', function () {
      cy.login()

      cy.visit(`/${this.PROJECT_ID}/visualizer`)

      cy.get('button[aria-label="Filters"]').click()

      cy.contains('Add Filters').click()

      cy.get('summary[aria-label="Analysis"]').click()

      cy.contains('boonai-text-detection').click()

      cy.contains('Add Filters').click()

      cy.contains('analysis.boonai-text-detection')

      cy.get('input[aria-label="Add Text Detection Filter"]')
        .type('improbable text that should never have results')
        .type('{enter}')

      cy.contains('All assets have been filtered out.')
    })

    it('can add a Prediction filter', function () {
      cy.login()

      cy.visit(`/${this.PROJECT_ID}/visualizer`)

      cy.get('button[aria-label="Filters"]').click()

      cy.contains('Add Filters').click()

      cy.get('summary[aria-label="Analysis"]').click()

      cy.contains('boonai-label-detection').click()

      cy.contains('Add Filters').click()

      cy.contains('analysis.boonai-label-detection')

      cy.contains('daisy').click()

      cy.selectFirstAsset()
    })

    it('can add a Date filter', function () {
      cy.login()

      cy.visit(`/${this.PROJECT_ID}/visualizer`)

      cy.get('button[aria-label="Filters"]').click()

      cy.contains('Add Filters').click()

      cy.get('summary[aria-label="System"]').click()

      cy.contains('timeModified').click()

      cy.contains('Add Filters').click()

      cy.contains('system.timeModified')

      cy.contains('exists').get('select').select('date')

      cy.contains('Reset').click()

      cy.selectFirstAsset()
    })
  })
})
