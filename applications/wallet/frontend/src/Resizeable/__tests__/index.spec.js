import TestRenderer from 'react-test-renderer'

import Resizeable from '..'

describe('<Resizeable />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(
      <Resizeable initialWidth={400} minWidth={52} storageName="yoga-div">
        Yoga div
      </Resizeable>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
