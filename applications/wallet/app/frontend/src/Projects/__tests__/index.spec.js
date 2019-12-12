import TestRenderer from 'react-test-renderer'

import Projects from '..'

jest.mock('../../Layout', () => () => 'Layout')

describe('<Projects />', () => {
  it('should render properly without data', () => {
    const component = TestRenderer.create(
      <Projects>{() => `Hello World`}</Projects>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with data', () => {
    require('swr').__setMockUseSWRResponse({
      data: { results: [{ id: '1', name: 'project-name' }] },
    })

    const component = TestRenderer.create(
      <Projects>{() => `Hello World`}</Projects>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
