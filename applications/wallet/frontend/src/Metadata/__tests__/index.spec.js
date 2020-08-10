import TestRenderer, { act } from 'react-test-renderer'

import asset from '../../Asset/__mocks__/asset'

import Metadata from '..'

jest.mock('../../Resizeable', () => 'Resizeable')
jest.mock('../../JsonDisplay', () => 'JsonDisplay')
jest.mock('../../MetadataPretty/Labels', () => 'MetadataPrettyLabels')
jest.mock('../../MetadataPretty/Row', () => 'MetadataPrettyRow')

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ASSET_ID = asset.id

describe('<Metadata />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(<Metadata />)

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render selected asset', () => {
    require('swr').__setMockUseSWRResponse({ data: asset })

    require('next/router').__setUseRouter({
      query: { id: ASSET_ID, projectId: PROJECT_ID },
    })

    const component = TestRenderer.create(<Metadata />)

    expect(component.toJSON()).toMatchSnapshot()

    // Open all Accordions
    act(() => {
      component.root
        .findAllByProps({ 'aria-label': 'Expand Section' })
        .forEach((section) => section.props.onClick())
    })

    act(() => {
      component.root.findByProps({ children: 'raw json' }).props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
