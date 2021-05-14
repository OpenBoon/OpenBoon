import asset from '../../Asset/__mocks__/asset'

import { formatDisplayName, formatDisplayValue, filter } from '../helpers'

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

  describe('formatDisplayValue()', () => {
    it('should format size', () => {
      expect(formatDisplayValue({ name: 'size', value: 28648 })).toEqual(
        '28.0 KB',
      )
    })

    it('should format size', () => {
      expect(
        formatDisplayValue({
          name: 'time',
          value: '2020-04-23T17:45:23.523104',
        }),
      ).toEqual('2020-04-23 17:45 UTC')
    })
  })

  describe('filter()', () => {
    it('should find an analysis module', () => {
      const filteredMetadata = filter({
        metadata: asset.metadata,
        searchString: 'defects',
      })

      expect(filteredMetadata.analysis).toEqual({
        'knn-surface-defects': asset.metadata.analysis['knn-surface-defects'],
      })
    })

    it('should filter all metadata', () => {
      const filteredMetadata = filter({
        metadata: asset.metadata,
        searchString: 'no results',
      })

      expect(filteredMetadata.analysis).toEqual({})
    })

    it('should handle any characters', () => {
      const filteredMetadata = filter({
        metadata: asset.metadata,
        searchString: '[',
      })

      expect(filteredMetadata.analysis).toEqual(asset.metadata.analysis)
    })
  })
})
