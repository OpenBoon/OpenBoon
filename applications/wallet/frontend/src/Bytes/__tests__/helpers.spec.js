import { bytesToSize } from '../helpers'

describe('bytesToSize()', () => {
  it('should format file sizes', () => {
    expect(bytesToSize({ bytes: 94566 })).toEqual('92.3 KB')
    expect(bytesToSize({ bytes: 945 })).toEqual('945 Bytes')
    expect(bytesToSize({ bytes: 0 })).toEqual('0 Bytes')
  })
})
