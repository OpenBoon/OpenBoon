import TestRenderer, { act } from 'react-test-renderer'

import asset from '../../Asset/__mocks__/asset'
import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import Metadata, { noop } from '..'

jest.mock('../../Resizeable', () => 'Resizeable')
jest.mock('../../JsonDisplay', () => 'JsonDisplay')
jest.mock('../PrettyRow', () => 'MetadataPrettyRow')

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ASSET_ID = asset.id

describe('<Metadata />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Metadata />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render selected asset', () => {
    require('swr').__setMockUseSWRResponse({ data: asset })

    require('next/router').__setUseRouter({
      query: { id: ASSET_ID, projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Metadata />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Open all Accordions
    act(() => {
      component.root
        .findAllByProps({ 'aria-label': 'Expand Section' })
        .forEach((section) => section.props.onClick())
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root.findByProps({ children: 'raw json' }).props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('noop should do nothing', () => {
    expect(noop()).toBe(undefined)
  })
})
