import useSWR from 'swr'
import { useRouter } from 'next/router'

import { colors, spacing, constants, typography } from '../Styles'

import Card from '../Card'

import ProjectUsageBar from './UsageBar'
import FlashMessage, { VARIANTS } from '../FlashMessage'

const IMG_WIDTH = 32

const ProjectUsagePlan = () => {
  const {
    query: { projectId },
  } = useRouter()

  const {
    data: { results: subscriptions },
  } = useSWR(`/api/v1/projects/${projectId}/subscriptions/`)

  if (subscriptions.length === 0) return null

  const {
    limits: { videoHours: videoLimit, imageCount: imageLimit },
    usage: { videoHours: videoUsage, imageCount: imageUsage },
    modules,
  } = subscriptions[0]

  const videoOverTime = videoUsage - videoLimit
  const imageOverTime = imageUsage - imageLimit

  return (
    <Card
      header="Project Usage Plan"
      content={
        <>
          {(videoOverTime > 1 || imageOverTime > 1) && (
            <div css={{ paddingBottom: spacing.comfy }}>
              <FlashMessage variant={VARIANTS.ERROR}>
                <div css={{ fontWeight: typography.weight.regular }}>
                  You are{' '}
                  {videoOverTime > 1 && (
                    <>
                      <strong>{videoOverTime} hours over</strong> your video
                      plan
                    </>
                  )}
                  {videoOverTime > 1 && imageOverTime > 1 && ' and '}
                  {imageOverTime > 1 && (
                    <>
                      <strong>{imageOverTime} assets over</strong> your image
                      &amp; documents plan
                    </>
                  )}
                  . Contact your Account Manager to add more resources.
                </div>
              </FlashMessage>
            </div>
          )}
          <div
            css={{
              borderBottom: constants.borders.tabs,
              paddingBottom: spacing.normal,
            }}
          >
            <h4
              css={{
                display: 'flex',
                alignItems: 'center',
                img: {
                  marginRight: spacing.moderate,
                },
              }}
            >
              <img src="/icons/videos.png" alt="" width={IMG_WIDTH} /> Video:{' '}
              {videoLimit.toLocaleString()} hours
            </h4>
            <ProjectUsageBar
              limit={videoLimit}
              usage={videoUsage}
              legend="/hrs"
            />
          </div>
          <div
            css={{
              borderBottom: constants.borders.tabs,
              paddingTop: spacing.normal,
              paddingBottom: spacing.normal,
            }}
          >
            <h4
              css={{
                display: 'flex',
                alignItems: 'center',
                img: {
                  marginRight: spacing.moderate,
                },
              }}
            >
              <img src="/icons/images.png" alt="" width={IMG_WIDTH} /> Image /
              Documents: {imageLimit.toLocaleString()}
            </h4>
            <ProjectUsageBar limit={imageLimit} usage={imageUsage} legend="" />
          </div>
          <div
            css={{
              borderBottom: constants.borders.tabs,
              paddingTop: spacing.normal,
              paddingBottom: spacing.normal,
            }}
          >
            <h4 css={{ paddingBottom: spacing.small }}>Additional Modules:</h4>

            {modules.length === 0 ? (
              <span css={{ color: colors.structure.zinc, fontStyle: 'italic' }}>
                None
              </span>
            ) : (
              <ul css={{ padding: 0, margin: 0, listStyle: 'none' }}>
                {modules.map((module) => (
                  <li
                    key={module}
                    css={{
                      fontFamily: 'Roboto Mono',
                      color: colors.structure.zinc,
                      paddingBottom: spacing.mini,
                    }}
                  >
                    {module}
                  </li>
                ))}
              </ul>
            )}
          </div>
          <div
            css={{ paddingTop: spacing.normal, color: colors.structure.zinc }}
          >
            Contact your Account Manager to add additional modules and resources
            to your plan.
          </div>
        </>
      }
    />
  )
}

export default ProjectUsagePlan
