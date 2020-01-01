import NoJobsSvg from '../Icons/noJobs.svg'

import { colors } from '../Styles'

const JobsEmpty = () => {
  return (
    <div
      css={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
      <NoJobsSvg width={40} color={colors.signal.warning.base} />
      <div>There are currently no jobs in the queue.</div>
      <div>Any new job will appear here.</div>
    </div>
  )
}

export default JobsEmpty
