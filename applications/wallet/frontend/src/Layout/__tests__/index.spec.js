import TestRenderer, { act } from 'react-test-renderer'

import mockUser from '../../User/__mocks__/user'

import Layout from '..'

jest.mock('../../ProjectSwitcher', () => 'ProjectSwitcher')
jest.mock('../../Sidebar', () => 'Sidebar')

const noop = () => () => {}

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

describe('<Layout />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]/jobs',
      query: { projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(
      <Layout user={mockUser} logout={noop}>
        Hello World
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
