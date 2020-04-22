import { formatDisplayName } from '../helpers'

describe('<Metadata /> helpers', () => {
  describe('formatDisplayName()', () => {
    it('should capitalize single words', () => {
      expect(formatDisplayName({ name: 'checksum' })).toEqual('Checksum')
    })
    it('should format url', () => {
      expect(formatDisplayName({ name: 'url' })).toEqual('URL')
    })
    it('should format IDs', () => {
      expect(formatDisplayName({ name: 'dataSourceId' })).toEqual(
        'Data Source ID',
      )
    })
    it('should names containing file', () => {
      expect(formatDisplayName({ name: 'filename' })).toEqual('File Name')
      expect(formatDisplayName({ name: 'files' })).toEqual('Files')
    })
  })
})
