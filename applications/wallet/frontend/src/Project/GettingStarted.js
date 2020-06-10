import PropTypes from 'prop-types'
import Link from 'next/link'

import { typography, colors, spacing, constants } from '../Styles'

import Card from '../Card'
import Button, { VARIANTS } from '../Button'

import DataSourcesSvg from '../Icons/datasources.svg'
import JobQueueSvg from '../Icons/jobQueue.svg'
import VisualizerSvg from '../Icons/visualizer.svg'

const ICON_WIDTH = 20

const STEPS = [
  {
    step: 1,
    title: 'Create a Data Source',
    module: (
      <>
        <DataSourcesSvg width={ICON_WIDTH} aria-hidden />
        Data Sources
      </>
    ),
    content:
      'To create a Data Source, connected to a bucket, ' +
      'choose which types of assets you’d like to process, ' +
      'and select which machine learning modules you’d like to apply to them.',
    cta: '+ Create a Data Source',
    link: '/[projectId]/data-sources/add',
  },
  {
    step: 2,
    title: 'Review Job Progress',
    module: (
      <>
        <JobQueueSvg width={ICON_WIDTH} aria-hidden />
        Job Queue
      </>
    ),
    content:
      'After a Data Source had been created a job kicks off and you can view ' +
      'its progress in the Job Queue. There you can and apply actions ' +
      'like “pause” “cancel” or “retry”. ' +
      'You can also inspect any errors that may have occurred.',
    cta: 'View Job Queue',
    link: '/[projectId]/jobs',
  },
  {
    step: 3,
    title: 'Inspect Your New Dataset',
    module: (
      <>
        <VisualizerSvg width={ICON_WIDTH} aria-hidden />
        Visualizer
      </>
    ),
    content:
      'Once a job has successfully completed then the assets will appear ' +
      'in the Visualizer.  There you can inspect your assets new data, ' +
      'view the dataset in a variety of meaningful ways, and XYZ',
    cta: 'Inspect Assets',
    link: '/[projectId]/visualizer',
  },
]

const ProjectGettingStarted = ({ projectId }) => {
  return (
    <Card
      header="Getting Started"
      content={STEPS.map(({ step, title, module, content, cta, link }) => (
        <div
          key={step}
          css={{ paddingBottom: step === STEPS.length ? 0 : spacing.comfy }}
        >
          <div
            css={{
              borderBottom: step === STEPS.length ? 0 : constants.borders.tabs,
            }}
          >
            <h4
              css={{
                fontWeight: typography.weight.regular,
                fontSize: typography.size.medium,
                lineHeight: typography.height.medium,
                color: colors.key.one,
              }}
            >
              <span css={{ fontWeight: typography.weight.bold }}>
                Step {step}:&nbsp;
              </span>
              {title}
            </h4>
            <h5
              css={{
                fontSize: typography.size.regular,
                lineHeight: typography.height.regular,
                fontWeight: typography.weight.bold,
                margin: 0,
                paddingTop: spacing.normal,
                display: 'flex',
                alignItems: 'center',
                svg: { marginRight: spacing.base },
              }}
            >
              {module}
            </h5>
            <p
              css={{
                margin: 0,
                paddingTop: spacing.normal,
                paddingBottom: spacing.normal,
                color: colors.structure.zinc,
              }}
            >
              {content}
            </p>
            <div
              css={{
                display: 'flex',
                paddingBottom: step === STEPS.length ? 0 : spacing.comfy,
              }}
            >
              <Link
                href={link}
                as={link.replace('[projectId]', projectId)}
                passHref
              >
                <Button variant={VARIANTS.SECONDARY_SMALL}>{cta}</Button>
              </Link>
            </div>
          </div>
        </div>
      ))}
    />
  )
}

ProjectGettingStarted.propTypes = {
  projectId: PropTypes.string.isRequired,
}

export default ProjectGettingStarted
