import TestRenderer from 'react-test-renderer'

import Projects from '..'

import projects from '../__mocks__/projects'

jest.mock('../../Layout')

const noop = () => () => {}

describe('<Projects />', () => {
  it('should render properly without data', () => {
    const component = TestRenderer.create(
      <Projects logout={noop}>{() => `Hello World`}</Projects>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with data', () => {
    require('swr').__setMockUseSWRResponse({
      data: { results: projects },
    })

    const component = TestRenderer.create(
      <Projects logout={noop}>{() => `Hello World`}</Projects>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
