import TestRenderer from 'react-test-renderer'

import videoAsset from '../__mocks__/videoAsset'
import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import Asset from '..'

jest.mock('../../Metadata', () => 'Metadata')

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ASSET_ID = 'srL8ob5cTpCJjYoKkqqfa2ciyG425dGi'
const QUERY_STRING =
  '?id=srL8ob5cTpCJjYoKkqqfa2ciyG425dGi&query=W3sidHlwZSI6ImZhY2V0IiwiYXR0cmlidXRlIjoibWVkaWEudHlwZSIsInZhbHVlcyI6eyJmYWNldHMiOlsidmlkZW8iXX19XQ=='

describe('<Asset />', () => {
  it('should render properly', () => {
    require('swr').__setMockUseSWRResponse({ data: videoAsset })

    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID, id: ASSET_ID, query: QUERY_STRING },
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Asset />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render vertical videos properly', () => {
    require('swr').__setMockUseSWRResponse({
      data: {
        metadata: {
          files: [
            {
              size: 26029481,
              name: 'video_450x360.mp4',
              mimetype: 'video/mp4',
              id:
                'assets/srL8ob5cTpCJjYoKkqqfa2ciyG425dGi/proxy/video_450x360.mp4',
              category: 'proxy',
              attrs: { frameRate: 25.0, frames: 3611, width: 250, height: 360 },
            },
          ],
        },
      },
    })

    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID, id: ASSET_ID, query: QUERY_STRING },
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Asset />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should handle no query', () => {
    require('swr').__setMockUseSWRResponse({ data: videoAsset })

    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID, id: ASSET_ID },
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Asset />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
