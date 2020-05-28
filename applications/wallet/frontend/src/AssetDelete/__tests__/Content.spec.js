import { noop } from '../Content'

describe('<AssetDeleteContent />', () => {
  it('noop should do nothing', () => {
    expect(noop()).toBe(undefined)
  })
})
