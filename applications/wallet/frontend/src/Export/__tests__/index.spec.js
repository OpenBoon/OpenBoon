import TestRenderer from 'react-test-renderer'

import Export from '..'

describe('<Export />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(<Export />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
