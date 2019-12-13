import TestRenderer from 'react-test-renderer'

import mockUser from '../../User/__mocks__/user'
import projects from '../__mocks__/projects'

import Projects from '..'

jest.mock('../../Layout')

const noop = () => () => {}

describe('<Projects />', () => {
  it('should render properly without data', () => {
    const component = TestRenderer.create(
      <Projects user={mockUser} logout={noop}>
        {() => `Hello World`}
      </Projects>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with no projects', () => {
    require('swr').__setMockUseSWRResponse({
      data: { results: [] },
    })

    const component = TestRenderer.create(
      <Projects user={mockUser} logout={noop}>
        {() => `Hello World`}
      </Projects>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with projects', () => {
    require('swr').__setMockUseSWRResponse({
      data: { results: projects.results },
    })

    const component = TestRenderer.create(
      <Projects user={mockUser} logout={noop}>
        {() => `Hello World`}
      </Projects>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
