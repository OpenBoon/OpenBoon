import { noop } from '..'

describe('<User />', () => {
  it('noop should do nothing', () => {
    expect(noop()).toBe(undefined)
  })
})
