import TestRenderer from 'react-test-renderer'

import videoAsset from '../__mocks__/videoAsset'
import docAsset from '../__mocks__/docAsset'
import tracks from '../__mocks__/tracks'
import mockUser from '../../User/__mocks__/user'

import User from '../../User'

import Asset from '..'

jest.mock('../../MetadataCues', () => 'MetadataCues')
jest.mock('../../Timeline', () => 'Timeline')

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ASSET_ID = 'srL8ob5cTpCJjYoKkqqfa2ciyG425dGi'
const QUERY_STRING =
  '?assetId=srL8ob5cTpCJjYoKkqqfa2ciyG425dGi&query=W3sidHlwZSI6ImZhY2V0IiwiYXR0cmlidXRlIjoibWVkaWEudHlwZSIsInZhbHVlcyI6eyJmYWNldHMiOlsidmlkZW8iXX19XQ=='

describe('<Asset />', () => {
  it('should render properly', () => {
    require('swr').__setMockUseSWRResponse({
      data: {
        ...docAsset,
        signedUrl: {
          uri: 'https://storage.googleapis.com/image.jpeg',
          mediaType: 'image/jpeg',
        },
      },
    })

    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID, assetId: ASSET_ID, query: QUERY_STRING },
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Asset />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render videos properly', () => {
    require('swr').__setMockUseSWRResponse({
      data: {
        ...videoAsset,
        signedUrl: {
          uri: 'https://storage.googleapis.com/video.mp4',
          mediaType: 'video/mp4',
        },
        tracks,
      },
    })

    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID, assetId: ASSET_ID, query: QUERY_STRING },
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Asset />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should handle no query', () => {
    require('swr').__setMockUseSWRResponse({
      data: {
        ...videoAsset,
        signedUrl: {
          uri: 'https://storage.googleapis.com/video.mp4',
          mediaType: 'video/mp4',
        },
      },
    })

    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID, assetId: ASSET_ID },
    })

    const component = TestRenderer.create(
      <User initialUser={mockUser}>
        <Asset />
      </User>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
