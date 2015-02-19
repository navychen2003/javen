require 'rbconfig'

module IRB
  WINDOZE = Config::CONFIG['host_os'] =~ /mswin|mingw/

  # Subclass of IRB so can intercept methods
  class HIRB < Irb
    def initialize
      # This is ugly.  Our 'help' method above provokes the following message
      # on irb construction: 'irb: warn: can't alias help from irb_help.'
      # Below, we reset the output so its pointed at /dev/null during irb
      # construction just so this message does not come out after we emit
      # the banner.  Other attempts at playing with the hash of methods
      # down in IRB didn't seem to work. I think the worst thing that can
      # happen is the shell exiting because of failed IRB construction with
      # no error (though we're not blanking STDERR)
      begin
        # Map the '/dev/null' according to the runing platform
        # Under Windows platform the 'dev/null' is not fully compliant with unix,
        # and the 'NUL' object need to be use instead.
        devnull = "/dev/null"
        devnull = "NUL" if WINDOZE 
        f = File.open(devnull, "w")
        $stdout = f
        super
      ensure
        f.close()
        $stdout = STDOUT
      end
    end

    def output_value
      # Suppress output if last_value is 'nil'
      # Otherwise, when user types help, get ugly 'nil'
      # after all output.
      if @context.last_value != nil
        super
      end
    end
  end
end
