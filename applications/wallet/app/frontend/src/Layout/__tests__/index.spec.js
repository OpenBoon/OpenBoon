import TestRenderer, { act } from 'react-test-renderer'

import Layout from '..'

import projects from '../../Projects/__mocks__/projects'

jest.mock('../../ProjectSwitcher', () => 'ProjectSwitcher')
jest.mock('../../Sidebar', () => 'Sidebar')

const noop = () => () => {}

describe('<Layout />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(
      <Layout results={projects.results} logout={noop}>
        {() => `Hello World`}
      </Layout>,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Open Sidebar Menu' })
        .props.onClick({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
