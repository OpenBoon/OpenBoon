import TestRenderer from 'react-test-renderer'

import StylesReset from '../Reset'

describe('<StylesReset />', () => {
  it('should not blow up', () => {
    const component = TestRenderer.create(<StylesReset />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
