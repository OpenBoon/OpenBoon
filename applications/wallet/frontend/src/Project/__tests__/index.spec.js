import TestRenderer from 'react-test-renderer'

import project from '../__mocks__/project'
import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import Project from '..'

describe('<Project />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      pathname: '/[projectId]',
      query: { projectId: project.id },
    })

    require('swr').__setMockUseSWRResponse({ data: project })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Project />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
