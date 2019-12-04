import TestRenderer from 'react-test-renderer'

import Jobs from '..'

const noop = () => () => {}

describe('<Jobs />', () => {
  it('should render properly without data', () => {
    const component = TestRenderer.create(<Jobs logout={noop} />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with data', () => {
    require('swr').__setMockUseSWRResponse({
      data: { results: [{ name: 'project-name' }] },
    })
    const component = TestRenderer.create(<Jobs logout={noop} />)

    expect(component.toJSON()).toMatchSnapshot()
  })
})
