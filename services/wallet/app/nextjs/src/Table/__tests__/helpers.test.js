import { getPageDescription } from '../helpers'

describe('getPageDescription', () => {
  describe('When the current page is not the last page', () => {
    it('Returns default string', () => {
      const pageSize = 5
      const pageIndex = 0
      const numRowsOnPage = 5
      const numRowsTotal = 6
      const canNextPage = true
      const defaultString = 'Jobs: 1-5 of 6'

      expect(
        getPageDescription({
          pageSize,
          pageIndex,
          numRowsOnPage,
          numRowsTotal,
          canNextPage,
        }),
      ).toBe(defaultString)
    })
  })
  describe('When the current page is the last page', () => {
    describe('Where is there one row', () => {
      it('Returns default string', () => {
        const pageSize = 5
        const pageIndex = 1
        const numRowsOnPage = 1
        const numRowsTotal = 6
        const canNextPage = false
        const defaultString = 'Jobs: 6 of 6'

        expect(
          getPageDescription({
            pageSize,
            pageIndex,
            numRowsOnPage,
            numRowsTotal,
            canNextPage,
          }),
        ).toBe(defaultString)
      })
    })
    describe('When there is more than one row', () => {
      it('Returns default string', () => {
        const pageSize = 5
        const pageIndex = 1
        const numRowsOnPage = 2
        const numRowsTotal = 7
        const canNextPage = false
        const defaultString = 'Jobs: 6-7 of 7'

        expect(
          getPageDescription({
            pageSize,
            pageIndex,
            numRowsOnPage,
            numRowsTotal,
            canNextPage,
          }),
        ).toBe(defaultString)
      })
    })
  })
})
